package ru.glavbot.AVRestreamer;
 
import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
//import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ru.glavbot.customLogger.AVLogger;
import ru.glavbot.gsmfr.Encoder;
import ru.glavbot.gsmfr.Gsm;


//import java.net.UnknownHostException;

//import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
//import android.util.Log;

public class AudioSender extends Thread{

	private String host;
	private int port;
	private String token;
	private boolean useGsm;
	

    private static final int SAMPLE_RATE = 44100;//all devices must eat that
	private static final int CHUNK_SIZE_BASE = 882;
	//private static final int CHUNK_SIZE_BASEX4 = CHUNK_SIZE_BASE*4;
	private static final int SIZEOF_SHORT = 2;
	private static final int SIZEOF_FLOAT = 4;
	private static final String eol = "\r\n";
	
	private static final int CHUNK_SIZE_SHORT = CHUNK_SIZE_BASE * SIZEOF_SHORT;
	
	private static final int CHUNK_SIZE_FINAL = 160;
	
	private static final int CHUNK_SIZE_FLOAT = CHUNK_SIZE_BASE * SIZEOF_FLOAT;

	private static final int STD_DELAY = 1000;  
     
	private volatile int packageMillis = 100;
	
	private static final int PACKAGE_TIME= 20;
	
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

			AVLogger.e("","",e);
			
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
	        Thread.currentThread().setPriority(7);
	        mChildHandler = new Handler() {

	        //	boolean isRunning = false;
	        	Encoder encoder = new Encoder();
	        	byte[] gsm = new byte[Gsm.GSM_SAMPLE_SIZE];
	        	
	        	
	        	Socket socket = null;
	            private boolean isPlaying = false;        	

	            private byte[] audioData= new byte[CHUNK_SIZE_SHORT];
	            private short[] listenAudioData = new short[CHUNK_SIZE_BASE];
	            private short[] preparedAudioData= new short[CHUNK_SIZE_FINAL];
	            AudioRecord recorder = null;
	            private DataOutputStream os=null;
	            private DataInputStream is=new DataInputStream(new ByteArrayInputStream(audioData));
	            private int bufferSize;
	            {
					bufferSize =AudioRecord.getMinBufferSize(SAMPLE_RATE,
	                        AudioFormat.CHANNEL_IN_MONO,
	                        AudioFormat.ENCODING_PCM_16BIT);
					bufferSize=bufferSize<CHUNK_SIZE_SHORT?CHUNK_SIZE_SHORT:bufferSize;
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
					
						
					AVLogger.v("avatar audio out","starting play");
					closeSocket();
						
					InetAddress addr = null;
					try {
						addr = InetAddress.getByName(host);
					} catch (Exception e) {
						AVLogger.e("", "", e);
						errorHandler.sendMessageDelayed(errorHandler.obtainMessage(AUDIO_OUT_ERROR),STD_DELAY);
						return;
					}
					//String dataToGet = (eol + eol);
					//String data = "";
					try {
						socket = new Socket(addr, port);
						
						socket.setKeepAlive(true);
						socket.setSoTimeout(3000);
						//socket.setSendBufferSize(CHUNK_SIZE_SHORTX4+10);
						socket.setSoLinger(true, 0);
						socket.setTcpNoDelay(true);
						OutputStream s = socket.getOutputStream();
						String ident = getToken();

						
						
						
						String header = String.format("POST /%s HTTP/1.1" + eol
								+ "Server: %s:%d" + eol
								+ "User-Agent: avatar/0.2" + eol + eol, ident,
								host, port);
						s.write(header.getBytes());
						s.flush();
						

						os = new DataOutputStream(s);

							
					} catch (IOException e) {
							closeSocket();
							errorHandler.sendMessageDelayed(errorHandler.obtainMessage(AUDIO_OUT_ERROR),STD_DELAY);
							return;
					}
							
					isPlaying=true;
					OnScreenLogger.setAudioOut(true);
					
					//queue.mark(MAX_QUEUE);
					recorder.startRecording();
					mChildHandler.obtainMessage(PROCESS_AUDIO).sendToTarget();
				}

				private void closeSocket()
				{
					try {
						if (socket != null)
							socket.close();
					} catch (IOException e) {
						AVLogger.w("avatar audio out", "error closing socket", e);
					}
					
					socket = null;
					isPlaying=false;
					resetQueue();
					OnScreenLogger.setAudioOut(false);
					AVLogger.v("avatar audio out","socket closed");
				}


				private void stopRecord() {
					mChildHandler.removeMessages(PROCESS_AUDIO);
					recorder.stop();
					closeSocket();
					//isPlaying=false;
					
				}

				private static final int MAX_QUEUE= Gsm.GSM_SAMPLE_SIZE*100;
				private int packagesInQueue=0;
				
				//private byte[] queueData= new byte[MAX_QUEUE];
				private ByteArrayOutputStream queue = new ByteArrayOutputStream(MAX_QUEUE);
				
				private void resetQueue()
				{
					packagesInQueue=0;
					queue.reset();
				}
				
				
				private void doRecord() {
					if(isPlaying)
					{
						try {
							int bytes_read=recorder.read(audioData, 0, CHUNK_SIZE_SHORT);
							if(bytes_read>0)
							{
								AVLogger.v("avatar audio out","read "+String.format("%d", bytes_read)+" bytes");
								
								// a) fix endianess
								/*byte tmp;
								for(int j=0;j<CHUNK_SIZE_BASE;j++)
								{
									tmp=audioData[j*2];
									audioData[j*2]=audioData[j*2+1];
									audioData[j*2+1]=tmp;
								}*/
								
								
								ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(listenAudioData);
								//b) create array of shorts
								//is.reset();
								//for(int i=0;i<CHUNK_SIZE_BASE;i++)
								//{
									
								//	try{
								//		listenAudioData[i] = is.readShort();
								//	}
								//	catch (EOFException e) {
								//		AVLogger.v("avatar audio out","sent "+String.format("%d", (i+1)*2)+" bytes");
								//		break;
								//	} 

								//}
								// c) downsampling
								for (int i=0; i<CHUNK_SIZE_FINAL;i++)
								{
									preparedAudioData[i]=listenAudioData[i*441/80];
								}
								
								
								
								if(useGsm)
								{
									encoder.encode(preparedAudioData, gsm);
									queue.write(gsm);
									packagesInQueue++;
									if(((packagesInQueue* PACKAGE_TIME)>packageMillis)||(packagesInQueue==100))
									{
										os.write(queue.toByteArray(),0,packagesInQueue*Gsm.GSM_SAMPLE_SIZE);
										os.flush();
										resetQueue();
									}
								}
								else
								{
									for (int i=0; i<CHUNK_SIZE_FINAL;i++)
									{
										
										os.writeShort(preparedAudioData[i]);
									}
									
								}
							}
							mChildHandler.obtainMessage(PROCESS_AUDIO).sendToTarget();
						} catch (IOException e) {
							AVLogger.e("","",e);
							//isPlaying=false;
							closeSocket();
							errorHandler.sendMessageDelayed(errorHandler.obtainMessage(AUDIO_OUT_ERROR),STD_DELAY);
						}
						
						
					}
					
				}

	        };
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
			}
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
	public boolean isUseGsm() {
		return useGsm;
	}


	public void setUseGsm(boolean useGsm) {
		this.useGsm = useGsm;
	}

	/*public int getRtt() {
		return rtt;
	}*/


	public void calcPackageMillis(int rtt) {
		packageMillis = rtt*2+100;
	}

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