package ru.glavbot.asyncHttpRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

import android.util.Log;

public class ReadAllConnectionRunner extends AbstractConnectionRunner {

	ReadAllConnectionRunner(ConnectionManager owner) {
		super(owner);
	}

	@Override
	protected AsyncRequestResponse processResponce(HttpResponse responce)
			throws IllegalStateException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(responce.getEntity()
				.getContent()));
		String line, result = "";
		AsyncRequestResponse  rr=null;
		try {
			while (((line = br.readLine()) != null)&&!isCancelled())
			{

					result += line;
			}
			 rr = new AsyncRequestResponse(responce.getStatusLine().getStatusCode(),result ,null);
		} catch (Exception e) {
				Log.e("", "", e);
				br.close();
				responce.getEntity().getContent().close();
				rr = new AsyncRequestResponse(AsyncRequestResponse.STATUS_INTERNAL_ERROR,null,e);
		}
		return rr;
	}

}
