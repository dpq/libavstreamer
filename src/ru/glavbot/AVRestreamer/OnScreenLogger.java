package ru.glavbot.AVRestreamer;

import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class OnScreenLogger {
	
	static OnScreenLogger instance;
	TextView textView;
	
	public static OnScreenLogger init(TextView textView)
	{
		if(instance==null)
		{
			return new OnScreenLogger(textView);
		}
		else
		{
			instance.textView=textView;
			return instance;
		}
			
	}
	
	
	OnScreenLogger (TextView textView)
	{
		instance=this;
		this.textView=textView;
	}
	
	volatile boolean audioIn=false;
	volatile boolean audioOut=false;
	volatile boolean videoIn=false;
	volatile boolean videoOut=false;
	volatile boolean commands=false;
	
	private static final String template="Commands: %s\nAudioIn: %s\nAudioOut: %s\nVideoIn: %s\nVideoOut: %s";
	private static final String yes="Yes";
	private static final String no="No";
	
	
	static void setVideoIn(boolean b)
	{
		instance.videoIn=b;
		instance.drawer.obtainMessage(DRAW_STATE).sendToTarget();
	}
	static void setVideoOut(boolean b)
	{
		instance.videoOut=b;
		instance.drawer.obtainMessage(DRAW_STATE).sendToTarget();
	}
	static void setAudioIn(boolean b)
	{
		instance.audioIn=b;
		instance.drawer.obtainMessage(DRAW_STATE).sendToTarget();
	}
	static void setAudioOut(boolean b)
	{
		instance.audioOut=b;
		instance.drawer.obtainMessage(DRAW_STATE).sendToTarget();
	}
	public static void setCommands(boolean b)
	{
		instance.commands=b;
		instance.drawer.obtainMessage(DRAW_STATE).sendToTarget();
	}
	private static final int DRAW_STATE = 1;
	
	Handler drawer = new Handler()
	{
		 public void handleMessage(Message msg) {
	         	
	         	switch (msg.what)
	         	{
	         		case DRAW_STATE:
	         			drawState();
	         			break;
	         		default:
	         			throw new RuntimeException("Unknown command to state writer thread");
	         	};
					
	         }

		private void drawState() {
			
			String data = String.format(template, instance.commands?yes:no,instance.audioIn?yes:no,instance.audioOut?yes:no,instance.videoIn?yes:no,instance.videoOut?yes:no);
			textView.setText(data);
		}
	};
	
}
