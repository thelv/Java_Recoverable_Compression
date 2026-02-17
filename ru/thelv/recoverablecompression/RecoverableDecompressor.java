package ru.thelv.recoverablecompression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class RecoverableDecompressor 
{   
	public enum Result 
	{
		SUCCESS, 
		DATA_CUTTED 
	}
	
	public static class RecoveryPoint
	{
		public long compressedN, decompressedN;
		
		public RecoveryPoint(long compressedN, long decompressedN)
		{
			this.compressedN=compressedN;
			this.decompressedN=decompressedN;			
		}
	}
	
	protected void onProgress(long bytesReaded, long bytesWrited)
	{
		//
	}
	
	protected void onBlockCompleted(RecoveryPoint recoveryPoint)
	{
		//
	}
	
	public RecoveryPoint getRecoveryPoint()
	{
		return recoveryPoint;
	}
     
	private RecoveryPoint recoveryPoint=new RecoveryPoint(0, 0);
	
	class BlockInputStream extends InputStream
	{
		private InputStream inputStream;
		private int blockSize=0, bytesReaded=0;
		
		int getBytesReaded()
		{
			return bytesReaded;
		}
		
		public BlockInputStream(InputStream inputStream)
		{
			this.inputStream=inputStream;
		}
		
		public void init(int blockSize)
		{
			this.blockSize=blockSize;
			bytesReaded=0;
		}
		
		@Override
		public int read() throws IOException 
		{
			byte[] buffer=new byte[1];
			int res=read(buffer, 0, 1);
			if(res>0)
			{
				return buffer[0] & 0xFF;
			}
			else 
			{	
				return res;
			}
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException 
		{			
			len=Math.min(len, blockSize-bytesReaded);
			if(len<=0) return -1;

			int lenActual=inputStream.read(b, off, len);
			bytesReaded+=lenActual;
			return lenActual;
		}			
		
		@Override
		public void close() throws IOException 
		{
			//
		}
		
		public void closeActual() throws IOException
		{
			super.close();
		}
	}   
   
    public Result start(InputStream inputStream, OutputStream outputStream, int readBufferSize, RecoveryPoint recoveryPoint) throws IOException
	{
		this.recoveryPoint=recoveryPoint;
		BlockInputStream blockInputStream=new BlockInputStream(inputStream);
		InputStreamFixSizeDataReader blockHeaderReader=new InputStreamFixSizeDataReader(inputStream, 4);		
		byte[] buffer=new byte[readBufferSize];
		while(true)
		{
			while(! blockHeaderReader.read());			
			switch(blockHeaderReader.getResult())
			{
				case InputStreamFixSizeDataReader.Result.SUCCESS:
					break;
					
				case InputStreamFixSizeDataReader.Result.NO_DATA:
					return Result.SUCCESS;
					
				case InputStreamFixSizeDataReader.Result.DATA_CUTTED:
					return Result.DATA_CUTTED;
					
				default:
					break;
			}
			int blockSize=ByteBuffer.wrap(blockHeaderReader.buffer).getInt();
			blockInputStream.init(blockSize);
			InputStream decompressor=decompressorCreate(blockInputStream);
			int bytesWrited=0;
			while(true)
			{
				int len=decompressor.read(buffer, 0, readBufferSize);
								
				if(len==-1)
				{
					decompressor.close();
					if(blockInputStream.getBytesReaded()<blockSize)
					{
						return Result.DATA_CUTTED;
					}
					this.recoveryPoint.decompressedN+=bytesWrited;
					this.recoveryPoint.compressedN+=blockSize+4;
					blockHeaderReader.reset();
					onBlockCompleted(recoveryPoint);
					break;
				}
				else
				{
					outputStream.write(buffer, 0, len);
					bytesWrited+=len;
					onProgress(this.recoveryPoint.compressedN+4+blockInputStream.getBytesReaded(), this.recoveryPoint.decompressedN+bytesWrited);
				}
			}
		}
    }
	
	abstract protected InputStream decompressorCreate(InputStream inputStream) throws IOException;              
    
    public byte[] stateSerialize() 
	{
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(recoveryPoint.compressedN);
        buffer.putLong(recoveryPoint.decompressedN);			
        return buffer.array();
    }
    
    public static RecoveryPoint stateDeserialize(byte[] data) 
	{
        if (data == null || data.length < 16) 
		{
            throw new IllegalArgumentException("Invalid state data");
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);
        long recoveryPointCompressedN=buffer.getLong();
        long recoveryPointDecompressedN=buffer.getLong();
		return new RecoveryPoint(recoveryPointCompressedN, recoveryPointDecompressedN);		
    }      
}