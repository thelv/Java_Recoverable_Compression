package ru.thelv.recoverablecompression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


class CloseShieldOutputStream extends OutputStream 
{
    private OutputStream base;
    
    CloseShieldOutputStream(OutputStream base) 
	{
        this.base = base;
    }
    
    @Override
    public void write(int b) throws IOException 
	{
        base.write(b);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException 
	{
        base.write(b, off, len);
    }
    
    @Override
    public void flush() throws IOException 
	{
        base.flush();
    }
    
    @Override
    public void close() throws IOException 
	{
        //
    }
}

class InputStreamFixSizeDataReader
{	
	public enum Result 
	{
		SUCCESS, 
		NO_DATA,
		DATA_CUTTED 
	}
	
	private Result result;
	public byte[] buffer;
	private int i=0, n;
	InputStream s;
	InputStreamFixSizeDataReader(InputStream s, int n)
	{
		buffer=new byte[n];
		this.s=s;
		this.n=n;
	}
	
	Result getResult()
	{
		return result;
	}
	
	int getI()
	{
		return i;
	}
	
	boolean read() throws IOException 
	{
		int r=s.read(buffer, i, n-i);
		if(r==-1) 
		{
			if(i==0) 
			{
				result=Result.NO_DATA;
			}
			else
			{
				result=Result.DATA_CUTTED;
			}
			return true;
		}
		
		i+=r;
		if(i==n)
		{
			result=Result.SUCCESS;
			return true;
		}		
		return false;
	}
	
	void reset()
	{
		i=0;
	}
}