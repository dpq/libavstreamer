package ru.glavbot.AVRestreamer;
 
import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;


//import java.net.UnknownHostException;

//import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class AudioSender extends Thread{

	private String host;
	private int port;
	private String token;
	
	

    private static final int SAMPLE_RATE = 44100;//all devices must eat that
	private static final int CHUNK_SIZE_BASE = 320;
	private static final int CHUNK_SIZE_BASEX4 = CHUNK_SIZE_BASE*4;
	private static final int SIZEOF_SHORT = 2;
	private static final int SIZEOF_FLOAT = 4;

	private static final int CHUNK_SIZE_SHORT = CHUNK_SIZE_BASE * SIZEOF_SHORT;
	private static final int CHUNK_SIZE_SHORTX4 = CHUNK_SIZE_SHORT*4;
	
	private static final int CHUNK_SIZE_FLOAT = CHUNK_SIZE_BASE * SIZEOF_FLOAT;

	private static final int STD_DELAY = 1000;  
      
    Object sync= new Object();
    
    public void setHostAndPort(String host, int port)
	{
		this.host = host;
		this.port = port;
	}
	
	
	public AudioSender()
	{
		start();
		try {
			synchronized(sync)
			{
				sync.wait();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			Log.e("","",e);
			
		}
	}
	
	private boolean isRecording=false;
	public void startVoice()
	{

		isRecording=true;
		internalStart();
	}
	
	protected void internalStart()
	{
		Message msg = mChildHandler.obtainMessage(START_AUDIO);
		mChildHandler.sendMessage(msg);
	}
	
	
	public void stopVoice()
	{

		isRecording=false;
		internalStop();
	}
	
	protected void internalStop()
	{
		Message msg = mChildHandler.obtainMessage(STOP_AUDIO);
		mChildHandler.removeMessages(START_AUDIO);
		mChildHandler.sendMessageAtFrontOfQueue(msg);
	}
	
	Handler mChildHandler;
	
	private static final int START_AUDIO=0;
	protected static final int PROCESS_AUDIO = 1;
	private static final int STOP_AUDIO=2;
	protected static final int AUDIO_OUT_ERROR = -2;


	
	 public void run() {

		 synchronized(sync){
			 setName("AudioSender");
	        Looper.prepare();
	        
	        mChildHandler = new Handler() {

	        //	boolean isRunning = false;
	        	
	        	Socket socket = null;
	            private boolean isPlaying = false;        	

	            private byte[] audioData= new byte[CHUNK_SIZE_SHORTX4];
	            AudioRecord recorder = null;
	            private DataOutputStream os=null;
	            private DataInputStream is=new DataInputStream(new ByteArrayInputStream(audioData));
	            private int bufferSize;
	            {
					bufferSize =AudioRecord.getMinBufferSize(SAMPLE_RATE,
	                        AudioFormat.CHANNEL_IN_MONO,
	                        AudioFormat.ENCODING_PCM_16BIT);
					bufferSize=bufferSize<CHUNK_SIZE_SHORTX4?CHUNK_SIZE_SHORTX4:bufferSize;
			        is.mark(CHUNK_SIZE_FLOAT*4);
				    recorder = new AudioRecord(AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
			                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
			                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
	            }
	            
	            public void handleMessage(Message msg) {
	            	
	            	switch (msg.what)
	            	{
	            		case START_AUDIO:
	            			startRecord();
	            			break;
	            		case PROCESS_AUDIO:
	            			doRecord();
	            			break;
	            		case STOP_AUDIO:
	            			stopRecord();
	            			break;
	            		default:
	            			throw new RuntimeException("Unknown command to video writer thread");
	            	};
					
	            }




				private void startRecord() {

					if(!isRecording)
						return;

					if (hasMessages(START_AUDIO)) {
						removeMessages(START_AUDIO);
					}
					if(isPlaying)
					{
						return;
					}
					
						
					Log.v("avatar audio out","starting play");
					closeSocket();
						
					InetAddress addr = null;
					try {
						addr = InetAddress.getByName(host);
					} catch (Exception e) {
						Log.e("", "", e);
						errorHandler.sendMessageDelayed(errorHandler.obtainMessage(AUDIO_OUT_ERROR),STD_DELAY);
						return;
					}
					try {
						socket = new Socket(addr, port);
						
						socket.setKeepAlive(true);
						socket.setSoTimeout(1000);
						socket.setSendBufferSize(CHUNK_SIZE_SHORTX4+10);
						socket.setSoLinger(true, 0);
						OutputStream s = socket.getOutputStream();
						String ident = getToken();
						s.write(ident.getBytes());
						s.flush();

						os = new DataOutputStream(s);

							
					} catch (IOException e) {
							closeSocket();
							errorHandler.sendMessageDelayed(errorHandler.obtainMessage(AUDIO_OUT_ERROR),STD_DELAY);
							return;
					}
							
					isPlaying=true;
					OnScreenLogger.setAudioOut(true);
					recorder.startRecording();
					mChildHandler.obtainMessage(PROCESS_AUDIO).sendToTarget();
				}

				private void closeSocket()
				{
					try {
						if (socket != null)
							socket.close();
					} catch (IOException e) {
						Log.e("", "", e);
					}
					socket = null;
					isPlaying=false;
					OnScreenLogger.setAudioOut(false);
					Log.v("avatar audio out","socket closed");
				}


				private void stopRecord() {
					mChildHandler.removeMessages(PROCESS_AUDIO);
					recorder.stop();
					closeSocket();
					//isPlaying=false;
					
				}




				private void doRecord() {
					if(isPlaying)
					{
						try {
							int bytes_read=recorder.read(audioData, 0, CHUNK_SIZE_SHORTX4);
							if(bytes_read>0)
							{
								Log.v("avatar audio out","read "+String.format("%d", bytes_read)+" bytes");
								byte tmp;
								for(int j=0;j<CHUNK_SIZE_BASEX4;j++)
								{
									tmp=audioData[j*2];
									audioData[j*2]=audioData[j*2+1];
									audioData[j*2+1]=tmp;
								}
								//os.write(audioData, 0, bytes_read);
								int i;
								is.reset();
								int ctr=0;
								for(i=0;i<CHUNK_SIZE_BASEX4;i++)
								{
									
									//if(tmpShort==0)tmpShort=1;
									try{
										short tmpShort = is.readShort();
										if(ctr==0)
										{
											os.writeShort(tmpShort);
										}
										ctr++;
										ctr=ctr%4;
											//os.writeFloat((float)is.readShort()/(float)Short.MAX_VALUE);
									}
									catch (EOFException e) {
										Log.v("avatar audio out","sent "+String.format("%d", (i+1)*2)+" bytes");
										break;
									} 

								}
							}
							mChildHandler.obtainMessage(PROCESS_AUDIO).sendToTarget();
						} catch (IOException e) {
							Log.e("","",e);
							//isPlaying=false;
							closeSocket();
							errorHandler.sendMessageDelayed(errorHandler.obtainMessage(AUDIO_OUT_ERROR),STD_DELAY);
						}
						
						
					}
					
				}

	        };
	        sync.notifyAll();
		 }
	        
	        Looper.loop();
	    }

	private String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	};
	private Handler errorHandler = new Handler()
    {
    	@Override
    	 public void handleMessage(Message msg) {
            	
            	switch (msg.what)
            	{
            		case AUDIO_OUT_ERROR:
            			if(isRecording)
            			{
            				mChildHandler.removeMessages(START_AUDIO);
            				internalStop();
            				internalStart();
            			}
            			break;
            		default:
            			throw new RuntimeException("Unknown command to audio sender error handler");
            	};
				
            }
    };
	
}