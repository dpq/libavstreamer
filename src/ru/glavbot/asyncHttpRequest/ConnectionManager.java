package ru.glavbot.asyncHttpRequest;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.http.AndroidHttpClient;

public class ConnectionManager {
	private ArrayList<ConnectionRequest> queue = new ArrayList<ConnectionRequest>();

	private AbstractConnectionRunner runner=null;
	public void push(ConnectionRequest request) {
		queue.add(request);
		if (runner==null)
			startNext();
	}

	private DefaultHttpClient client = new DefaultHttpClient(); //AndroidHttpClient.newInstance("avatar/0.2");
	
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
			runner.execute(next);
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
			mgr.closeIdleConnections(1, TimeUnit.MINUTES);
			//client.getConnectionKeepAliveStrategy()
			//else
			//	throw new RuntimeException("Running task considered immortal. Kill it by throwing your tab into the trash");
		}
		startNext();
	}

	public HttpClient getClient() {
		return client;
	}


}
