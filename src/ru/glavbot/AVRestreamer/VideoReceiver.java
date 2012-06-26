package ru.glavbot.AVRestreamer;

import android.view.SurfaceView;


//import ru.glavbot.avatarProto.OnScreenLogger;
//import de.mjpegsample.MjpegView.MjpegView;

public class VideoReceiver {
	private SurfaceView view;
	private String token;
	//private String address;
	private String host;
	private int port;
	boolean isPlaying=false;
	
	VideoReaderThread reader;
	VideoDrawerThread drawer;
	
	public void setAddress(String host, int videoPort)
	{
		
		
		this.host = host;
		this.port = videoPort;
	}
	
	public VideoReceiver(SurfaceView view)
	{
		this.view=view;
		drawer= new VideoDrawerThread(view);
		reader= new VideoReaderThread();
		reader.setDrawerHandler(drawer.getChildHandler());
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}

	public void startReceiveVideo()
	{
		if(token.length()==0)
		{
			throw new RuntimeException("VideoReceiver started without token!");
		}
		//view.requestRead(String.format(address, token));
		//MjpegInputStream.read(,);
		OnScreenLogger.setVideoIn(true);

	}
	
	public void stopReceiveVideo()
	{
		//view.stopPlayback();
		OnScreenLogger.setVideoIn(false);
	}
	
	
//http://dev.glavbot.ru/restreamer?oid=web-~~TOKEN~~
}