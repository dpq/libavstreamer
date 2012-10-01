package ru.glavbot.AVRestreamer;

//import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Thread;
import java.net.InetAddress;
import java.net.Socket;

import ru.glavbot.customLogger.AVLogger;

//import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
//import android.util.Log;

public class AudioReceiver extends Thread {

	private String host;
	private int port;
	private String token;
	private static final String eol = "\r\n";
	AudioTrack player = null;
	private static final int SAMPLE_RATE = 11025;
	private static final int CHUNK_SIZE_BASE = 320;
	private static final int CHUNK_SIZE_BASEX4 = CHUNK_SIZE_BASE*4;
//	private static final int SIZEOF_SHORT = 2;
	private static final int SIZEOF_FLOAT = 4;

//	private static final int CHUNK_SIZE_SHORT = CHUNK_SIZE_BASE * SIZEOF_SHORT;
	private static final int BUFF_SIZE = CHUNK_SIZE_BASE * SIZEOF_FLOAT*2;
	private static final int STD_DELAY = 5000;


	
	Object sync = new Object();

	public void setHostAndPort(String host, int port)
	{
		this.host = host;
		this.port = port;
	}
	
	
	public AudioReceiver() {
		start();
		try {
			synchronized (sync) {
				sync.wait();
			}
		} catch (InterruptedException e) {

			AVLogger.e("", "", e);

		}
	}

	public void startVoice() {
		isRecording = true;
		internalStart();
	}

	protected void internalStart() {

		Message msg = mChildHandler.obtainMessage(START_AUDIO);
		mChildHandler.sendMessage(msg);
		AVLogger.v("avatar audio in","internal start");
	}

	public void stopVoice() {
		isRecording = false;
		internalStop();
	}

	protected void internalStop() {
		
		Message msg = mChildHandler.obtainMessage(STOP_AUDIO);
		mChildHandler.removeMessages(START_AUDIO);
		mChildHandler.sendMessageAtFrontOfQueue(msg);
		AVLogger.v("avatar audio in","internal stop");
	}

	Handler mChildHandler;

	private static final int START_AUDIO = 0;
	protected static final int PROCESS_AUDIO = 1;
	private static final int STOP_AUDIO = 2;
	protected static final int AUDIO_IN_ERROR = -3;
	private volatile boolean isPlaying = false;
	private boolean isRecording = false;

	public void run() {

		synchronized (sync) {
			Looper.prepare();
			setName("AudioReceiver");
			Thread.currentThread().setPriority(7);
			mChildHandler = new Handler() {

				Socket socket = null;

				private short[] shortAudioData = new short[CHUNK_SIZE_BASE];
				
				private int bufferSize;
				{
					bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
					bufferSize = CHUNK_SIZE_BASE > bufferSize ? CHUNK_SIZE_BASE: bufferSize;
				}
				
				DataInputStream floatStream;
				
				public void handleMessage(Message msg) {

					switch (msg.what) {
					case START_AUDIO:
						startPlay();
						break;
					case PROCESS_AUDIO:
						doPlay();
						break;
					case STOP_AUDIO:
						stopPlay();
						break;
					default:
						throw new RuntimeException("Unknown command to video writer thread");
					};

				}

				private void startPlay() {

					if (hasMessages(START_AUDIO)) {
						removeMessages(START_AUDIO);
					}
					if (isPlaying) {
						return;
					}

					AVLogger.v("avatar audio in", "starting play");
					closeSocket();

					InetAddress addr = null;
					try {
						addr = InetAddress.getByName(host);
					} catch (Exception e) {
						AVLogger.e("", "", e);
						errorHandler.sendMessageDelayed(
								errorHandler.obtainMessage(AUDIO_IN_ERROR),
								STD_DELAY);
						return;
					}
					String dataToGet = (eol + eol);
					String data = "";
					try {
						socket = new Socket(addr, port);
						socket.setKeepAlive(true);
						socket.setSoTimeout(4000);
						//socket.setReceiveBufferSize(BUFF_SIZE);
						socket.setSoLinger(true, 0);
						OutputStream s = socket.getOutputStream();
						String ident = getToken();

						String header = String.format("GET /%s HTTP/1.1" + eol
								+ "Server: %s:%d" + eol
								+ "User-Agent: avatar/0.2" + eol + eol, ident,
								host, port);
						
						try
						{
							s.write(header.getBytes());
							s.flush();
						}catch (IOException e) {
							AVLogger.e("avatar audio in","Error sending http get request! "+data);
							throw e;
						}
						
						
						floatStream = new DataInputStream( new BufferedInputStream(
								socket.getInputStream()));

						while (!(data.contains(dataToGet)
								|| (data.length() >= 1000) || interrupted() || hasMessages(STOP_AUDIO))) {
							data += (char) floatStream.readByte();
						}
						if (!data.contains(dataToGet)) {
							throw new IOException(
									"Incorrect answer to http request!!!");
						}

					} catch (IOException e) {
						if (!data.contains(dataToGet)) {
							AVLogger.e("avatar audio in","\\r\\n not found in string "+data);
						closeSocket();
						errorHandler.sendMessageDelayed(
								errorHandler.obtainMessage(AUDIO_IN_ERROR),
								STD_DELAY);
						}
						return;
					}

					isPlaying = true;
					OnScreenLogger.setAudioIn(true);
					player = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
							SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
							AudioFormat.ENCODING_PCM_16BIT, bufferSize,
							AudioTrack.MODE_STREAM);
					player.play();
					mChildHandler.obtainMessage(PROCESS_AUDIO).sendToTarget();

				}

				private void stopPlay() {
					
					AVLogger.v("avatar audio in","stopping play");
					mChildHandler.removeMessages(PROCESS_AUDIO);
					mChildHandler.removeMessages(STOP_AUDIO);
					
					if (player != null) {
						player.stop();
						player.release();
						player = null;
					}
					closeSocket();
					
				}

				private void closeSocket()
				{
					try {
						if (socket != null)
						{
							socket.close();
							
						}
					} catch (IOException e) {
						AVLogger.e("", "", e);
					}
					socket = null;
					isPlaying = false;
					OnScreenLogger.setAudioIn(false);
				}
				
				private void doPlay() {
					mChildHandler.removeMessages(PROCESS_AUDIO);
					if (isPlaying) {
							int dataRead;
							
							for (dataRead = 0; dataRead < CHUNK_SIZE_BASE; dataRead++) {
								try {
									short curr = (short) floatStream.readShort();//(floatStream.readFloat() * (float) Short.MAX_VALUE);
									//for(int i=0;i<4;i++)
									//{
									shortAudioData[dataRead/**4+i*/] = curr;
									//}
								} catch (IOException e1) {
									AVLogger.e("", "", e1);
									
									closeSocket();
									errorHandler.sendMessageDelayed(errorHandler.obtainMessage(AUDIO_IN_ERROR),STD_DELAY);
									break;
								}
							}
							
							if (dataRead > 0) {
								AVLogger.v("avatar audio in" ,String.format("readed %d bytes",dataRead*2));
								player.write(shortAudioData, 0, dataRead);
							}
							
							if(isPlaying)
							{
								mChildHandler.obtainMessage(PROCESS_AUDIO).sendToTarget();
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

	private Handler errorHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case AUDIO_IN_ERROR:
				errorHandler.removeMessages(AUDIO_IN_ERROR);
				if (isRecording) {
					mChildHandler.removeMessages(START_AUDIO);
					AVLogger.v("avatar audio in","reconnecting on error");
					internalStop();
					internalStart();
				}
				break;
			default:
				throw new RuntimeException(
						"Unknown command to incoming video error handler");
			}
			

		}
	};

}
