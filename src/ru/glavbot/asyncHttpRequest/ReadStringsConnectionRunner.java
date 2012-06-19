package ru.glavbot.asyncHttpRequest;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;

import org.apache.http.HttpResponse;

import android.util.Log;

public class ReadStringsConnectionRunner extends AbstractConnectionRunner {

	ReadStringsConnectionRunner(ConnectionManager owner) {
		super(owner);
		// TODO Auto-generated constructor stub
	}

	private static final int MAX_STRING=65536;
	
    private int findByte(BufferedReader in, byte sequence) throws IOException {
       // int seqIndex = 0;
        byte c;
        for(int i=0; i < MAX_STRING; i++) {
            c = (byte) in.read();
            if(c == sequence) {
                 return i;
            }
        }
        return -1;
    }
	
	
	@Override
	protected AsyncRequestResponse processResponce(HttpResponse response)
			throws IllegalStateException, IOException {
		// TODO Auto-generated method stub
		DataInputStream stream= new DataInputStream(response.getEntity().getContent());
		//BufferedReader br = new BufferedReader(new InputStreamReader(stream));
	//	br.mark(MAX_STRING);
		String line="";
		AsyncRequestResponse  rr=null;
		//int code = responce.getStatusLine().getStatusCode();
		//if(code>0)
		//String stringAnswer;
		try {
			while (!isCancelled())
			{
				char c;
				
			//	try
			//	{
					c=(char)stream.readByte();
				
					if(c=='\n')
					{
						publishProgress(new AsyncRequestResponse(AsyncRequestResponse.STATUS_PROGRESS,line,null ));
						line="";
					}else 
					if(c==(byte)'\r')// nothing to do just skip
					{}
					else
					{
						line+=c;
					}
			//	}
			//	catch (SocketException e)
			//	{
			//		throw new Exception("socket closed");
			//	}
			//	catch (IOException e1)
			//	{
			//	
			//		Thread.sleep(5);
			//	}
				
				
			}
			rr = new AsyncRequestResponse(response.getStatusLine().getStatusCode(),null,null);
		} catch (Exception e) {
				Log.e("", "", e);
				stream.close();
				response.getEntity().consumeContent();
				rr = new AsyncRequestResponse(AsyncRequestResponse.STATUS_INTERNAL_ERROR,null,e);
		}
		return rr;
	}

}
