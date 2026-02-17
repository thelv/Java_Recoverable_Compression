package ru.thelv.recoverablecompression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public abstract class RecoverableCompressor
{					
	public RecoverableCompressor(InputStream inputStream, OutputStream outputStream, int blockSize) throws IOException
	{					
		InputStreamFixSizeDataReader blockReader=new InputStreamFixSizeDataReader(inputStream, blockSize);						
		OutputStream compressor;		
		byte[] decompressedBuffer=new byte[blockSize];
		while(true)
		{
			blockReader.reset();
			while(! blockReader.read());
			if(blockReader.getResult()==InputStreamFixSizeDataReader.Result.NO_DATA)
			{
				break;
			}
			
			int blockSizeActual=blockReader.getI();
			ByteArrayOutputStream buffer=new ByteArrayOutputStream();
			compressor=compressorCreate(buffer);			
			compressor.write(blockReader.buffer, 0, blockSizeActual);
			compressor.close();
			outputStream.write(ByteBuffer.allocate(4).putInt(buffer.size()).array());
			outputStream.write(buffer.toByteArray(), 0, buffer.size());
		}
	}

	abstract protected OutputStream compressorCreate(OutputStream outputStream) throws IOException;
}