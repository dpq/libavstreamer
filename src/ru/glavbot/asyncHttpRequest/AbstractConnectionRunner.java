package ru.glavbot.asyncHttpRequest;


import java.io.IOException;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;





import android.os.AsyncTask;
import android.util.Log;

abstract class AbstractConnectionRunner extends AsyncTask<ConnectionRequest,AsyncRequestResponse,AsyncRequestResponse> {

	ConnectionRequest request=null;
	
	ConnectionManager owner;
	
	HttpResponse response= null;
	
	protected boolean needConsume=true;
	
	protected void consumeCurrentResponce()
	{
		if(response!=null)
		{
			try {
				response.getEntity().consumeContent();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		HttpConnectionParams.setSoTimeout(client.getParams(), request.getTimeout());
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
					response.getEntity().consumeContent();
				} catch (IOException e1) {
					Log.e("","",e1);
				}
			}
		} catch (Exception e) {
			

			if(response!=null)
			{
				try {
					//response.getEntity().getContent().close();
					response.getEntity().consumeContent();
					
				} catch (IllegalStateException e1) {
					// TODO Auto-generated catch block
					Log.e("","",e1);
					
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					Log.e("","",e1);
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
