import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import ru.thelv.recoverablecompression.*;

public class Main
{
	public static void main(String[] args) throws IOException  
	{		
		//compress
		
        InputStream inputStreamOrig=new FileInputStream(new File("orig.txt"));
        OutputStream outputStreamCompressed=new FileOutputStream(new File("comp.txt"));
			
        new RecoverableCompressorGZIP(inputStreamOrig, outputStreamCompressed, 5);        
			
		inputStreamOrig.close();
		outputStreamCompressed.close();
	
		//create cutted compressed file
	
		byte[] compressedBytes=Files.readAllBytes(Paths.get("comp.txt"));
		byte[] compressedCuttedBytes=Arrays.copyOf(compressedBytes, Math.min(compressedBytes.length, 35));
		Files.write(Paths.get("comp_cutted.txt"), compressedCuttedBytes);
			
		//start decompression from cutted file
	
		InputStream inputStreamCompressedCutted=new FileInputStream(new File("comp_cutted.txt"));
		OutputStream outputStreamDecompressed=new FileOutputStream(new File("decomp.txt"));
	
		RecoverableDecompressor z=new RecoverableDecompressorGZIP()
		{
			@Override
			protected void onProgress(long bytesReaded, long bytesWrited)
			{
				System.out.printf("progress: %d %d\n", bytesReaded, bytesWrited);
			}
			
			@Override 
			protected void onBlockCompleted(RecoverableDecompressor.RecoveryPoint recoveryPoint) 
			{
				System.out.printf("block completed: %d %d\n", recoveryPoint.compressedN, recoveryPoint.decompressedN);
			}
		};
		RecoverableDecompressor.Result res;
		try
		{
			res=z.start(inputStreamCompressedCutted, outputStreamDecompressed, 2, new RecoverableDecompressor.RecoveryPoint(0, 0));
		}
		catch(Exception e)
		{
			res=RecoverableDecompressor.Result.DATA_CUTTED;
		}			
		
		switch(res)			
		{
			case SUCCESS:
				System.out.println("success on first try");
				return;
			
			case DATA_CUTTED:
				System.out.println("data cutted on first try");
				break;
			
			default:
				break;				
		}						
			
		inputStreamCompressedCutted.close();
		outputStreamDecompressed.close();
			
		//continue decompresion from recovery point
			
		byte[] state=z.stateSerialize();				
		RecoverableDecompressor.RecoveryPoint recoveryPoint=RecoverableDecompressor.stateDeserialize(state);
		//Or if right from the object
		//***
		//RecoverableDecompressor.RecoveryPoint recoveryPoint=z.getRecoveryPoint();
		//***
		System.out.printf("recovery point: %d %d\n", recoveryPoint.compressedN, recoveryPoint.decompressedN);

		//skip bytes in input file

		InputStream inputStreamCompressed=new FileInputStream(new File("comp.txt"));		
		inputStreamCompressed.skipNBytes(recoveryPoint.compressedN);					
	
		//cut tail of output file
	
		RandomAccessFile m=new RandomAccessFile("decomp.txt", "rw");
		m.setLength(recoveryPoint.decompressedN);
		m.close();	
			
		outputStreamDecompressed=new FileOutputStream(new File("decomp.txt"), true);
				
		try
		{
			res=z.start(inputStreamCompressed, outputStreamDecompressed, 2, recoveryPoint);
		}
		catch(Exception e)
		{
			res=RecoverableDecompressor.Result.DATA_CUTTED;
		}
				
		switch(res)			
		{
			case SUCCESS:
				System.out.println("success on second try");
				return;
			
			case DATA_CUTTED:
				System.out.println("data cutted on second try");
				break;
			
			default:
				break;				
		};

		inputStreamCompressed.close();
		outputStreamDecompressed.close();
	}
}

class RecoverableDecompressorGZIP extends RecoverableDecompressor
{
	@Override
	protected InputStream decompressorCreate(InputStream inputStream) throws IOException
	{		
		return new GZIPInputStream(inputStream);
	}
}

class RecoverableCompressorGZIP extends RecoverableCompressor
{
	@Override
	protected OutputStream compressorCreate(OutputStream outputStream) throws IOException
	{		
		return new GZIPOutputStream(outputStream);
	}
	
	RecoverableCompressorGZIP(InputStream inputStream, OutputStream outputStream, int blockSize) throws IOException
	{
		super(inputStream, outputStream, blockSize);
	}
}