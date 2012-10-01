package ru.glavbot.asyncHttpRequest;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;

public class ConnectionManager {
	private ArrayList<ConnectionRequest> queue = new ArrayList<ConnectionRequest>();

	private AbstractConnectionRunner runner=null;
	public void push(ConnectionRequest request) {
		queue.add(request);
		if (runner==null)
			startNext();
	}

	
	private static ClientConnectionManager cm;
	private static HttpParams params;
	
	
	static{
    SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(
            new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    
   	params = new BasicHttpParams();
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
    HttpProtocolParams.setContentCharset(params, "utf-8");
    // Create an HttpClient with the ThreadSafeClientConnManager.
    // This connection manager must be used if more than one thread will
    // be using the HttpClient.
    cm = new ThreadSafeClientConnManager(params, schemeRegistry);
	
	}
	
	
	private DefaultHttpClient client = new DefaultHttpClient(cm,params); //AndroidHttpClient.newInstance("avatar/0.2");
	
	private void startNext() {
		if(runner!=null)
			throw new RuntimeException("Makaronas i spagetti");
		
		if (!queue.isEmpty()) {
			ConnectionRequest next = queue.get(0);
			{
				switch(next.getProcessingType())
				{
				case ConnectionRequest.READ_ALL:
					runner = new ReadAllConnectionRunner(this);
					break;
				case ConnectionRequest.READ_STRINGS_ONE_BY_ONE:
					runner = new ReadStringsConnectionRunner(this);
					break;
				case ConnectionRequest.RETURN_REQUEST_ENTITY:
					runner = new ReturnEntityConnectionRunner(this);
					break;
				default:
					throw new RuntimeException("ConnectionManager::startNext: wrong request type!");
				}
			//runner = new AbstractConnectionRunner(this);
			
			}
			runner.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, next);
		}
	}

	public void stopCurrent() {
		if(runner!=null)
		{
			
			runner.cancel(true);
			runner.consumeCurrentResponceIfNeeded();
			runner=null;
			queue.remove(0);
			ClientConnectionManager mgr =client.getConnectionManager();
			mgr.closeExpiredConnections ();
			//client.getConnectionKeepAliveStrategy()
			//else
			//	throw new RuntimeException("Running task considered immortal. Kill it by throwing your tab into the trash");
		}
		startNext();
	}
	
	public void clearQueue() {
		if(runner!=null)
		{
			
			runner.cancel(true);
			runner.consumeCurrentResponceIfNeeded();
			runner=null;
			queue.remove(0);
			ClientConnectionManager mgr =client.getConnectionManager();
			mgr.closeIdleConnections(1, TimeUnit.MINUTES);
			//client.getConnectionKeepAliveStrategy()
			//else
			//	throw new RuntimeException("Running task considered immortal. Kill it by throwing your tab into the trash");
		}
		queue.clear();
	}

	public HttpClient getClient() {
		return client;
	}


}
