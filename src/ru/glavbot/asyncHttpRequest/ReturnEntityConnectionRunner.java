package ru.glavbot.asyncHttpRequest;

import java.io.IOException;

import org.apache.http.HttpResponse;

import android.util.Log;

public class ReturnEntityConnectionRunner extends AbstractConnectionRunner {

	ReturnEntityConnectionRunner(ConnectionManager owner) {
		super(owner);
		needConsume=false;
	}

	@Override
	protected AsyncRequestResponse processResponce(HttpResponse responce)
			throws IllegalStateException, IOException {
		// TODO Auto-generated method stub
		AsyncRequestResponse  rr=null;
		try {
			
			 rr = new AsyncRequestResponse(responce.getStatusLine().getStatusCode(),responce.getEntity() ,null);
		} catch (Exception e) {
				Log.e("", "", e);
				responce.getEntity().consumeContent();
				rr = new AsyncRequestResponse(AsyncRequestResponse.STATUS_INTERNAL_ERROR,null,e);
		}
		return rr;
	}

}
