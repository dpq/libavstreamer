package ru.glavbot.asyncHttpRequest;


import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import ru.glavbot.customLogger.AVLogger;
//import ru.glavbot.customLogger.AVLogger;
import android.os.AsyncTask;


abstract class AbstractConnectionRunner extends AsyncTask<ConnectionRequest,AsyncRequestResponse,AsyncRequestResponse> {

	ConnectionRequest request=null;
	
	ConnectionManager owner;
	
	HttpResponse response= null;
	
	protected boolean needConsume=true;
	
	protected synchronized void consumeCurrentResponce()
	{
		needConsume=false;
		if(response!=null)
		{
			try {
				response.getEntity().consumeContent();
			} catch (IOException e) {
				
				//e.printStackTrace();
			} 


		}
	}
	
	public void consumeCurrentResponceIfNeeded()
	{
		if(needConsume)
		{
			consumeCurrentResponce();
		}
	}
	
	
	
	AbstractConnectionRunner(ConnectionManager owner)
	{
		this.owner=owner;
		if (owner==null)
			throw new RuntimeException("Go to hell, owner should not be null");
	}
	
	@Override
	protected AsyncRequestResponse doInBackground(ConnectionRequest... params) {
		request = params[0];
		HttpClient client = owner.getClient();
		HttpParams p =client.getParams();
		HttpConnectionParams.setSoTimeout(p, request.getTimeout());
		HttpConnectionParams.setConnectionTimeout(p, request.getTimeout());
		
		AsyncRequestResponse asyncResponce=null;
		try {
			
			switch (request.getMethod()) {
			case ConnectionRequest.GET:
				response = client.execute(new HttpGet(request.getUrl()));
				break;
			case ConnectionRequest.POST:
				HttpPost httpPost = new HttpPost(request.getUrl());
				httpPost.setEntity(new StringEntity(request.getData()));
				response = client.execute(httpPost);
				break;
			case ConnectionRequest.PUT:
				HttpPut httpPut = new HttpPut(request.getUrl());
				httpPut.setEntity(new StringEntity(request.getData()));
				response = client.execute(httpPut);
				break;
			case ConnectionRequest.DELETE:
				response = client.execute(new HttpDelete(request.getUrl()));
				break;
			}
			int rcode=response.getStatusLine().getStatusCode();
			if(rcode>=200&&rcode<300)
			{
				asyncResponce=processResponce(response);
			}
			else
			{
				asyncResponce = new AsyncRequestResponse(rcode,null,null);
				try {
					consumeCurrentResponce();
				} 
				catch (IllegalStateException e1) {
					AVLogger.e("","",e1);
				}
			}
		} catch (Exception e) {
			

			if(response!=null)
			{
				try {
					//response.getEntity().getContent().close();
					consumeCurrentResponce();
					
				} catch (IllegalStateException e1) {
					
					AVLogger.e("","",e1);
					
				} 
			}
			asyncResponce= new AsyncRequestResponse(AsyncRequestResponse.STATUS_INTERNAL_ERROR,null,e);
		}
		
		return asyncResponce;
	}

	protected abstract AsyncRequestResponse processResponce(HttpResponse responce/*, boolean readAll*/) throws IllegalStateException,
	IOException;
	@Override
	protected void onProgressUpdate (AsyncRequestResponse... values)
	{
		IProcessAsyncRequestResponse progressProcessor = request.getProgressProcessor();
		if(progressProcessor!=null)
		{
			progressProcessor.processAsyncRequestResponse(values[0]);
		}
	}
	@Override
	protected void onPostExecute (AsyncRequestResponse result)
	{
		owner.stopCurrent();
		IProcessAsyncRequestResponse answerProcessor = request.getAnswerProcessor();
		if(answerProcessor!=null)
		{
			answerProcessor.processAsyncRequestResponse(result);
		}
	}
}
