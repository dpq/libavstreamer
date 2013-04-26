package ru.glavbot.AVRestreamer;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import ru.glavbot.customLogger.AVLogger;



import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
//import android.util.Log;

public class VideoReaderThread extends Thread {
	private String host;
	private int port;
	private String token;
	private String tag="anonym"; 
	private static final String eol = "\r\n";
	
	private static final int STD_DELAY = 1000;
	
    private final byte[] SOI_MARKER = { (byte) 0xFF, (byte) 0xD8 };
    private final byte[] EOF_MARKER = { (byte) 0xFF, (byte) 0xD9 };
    private final byte[] CONTENT_LENGTH = {'C','o','n','t','e','n','t','-','L','e','n','g','t','h'};
    private final byte[] COLON ={':'};
    private final byte[] EOL_MARKER = { (byte)0x0D,(byte) 0x0A };
    protected byte[] BOUNDARY = {'-','-','b','o','u','n','d','a','r','y','d','o','n','o','t','c','r','o','s','s',(byte)0x0D,(byte) 0x0A};
    private final static int HEADER_MAX_LENGTH = 100;
    private final static int JPEG_MAX_LENGTH = 4000000;
    private final static int FRAME_MAX_LENGTH = JPEG_MAX_LENGTH + HEADER_MAX_LENGTH;
    private int mContentLength = -1;
    private static final String  BOUNDARY_HEADER="boundary=";
    private Handler drawerHandler;
	Object sync = new Object();
    
    protected void setDrawerHandler(Handler drawerHandler)
    {
    	this.drawerHandler=drawerHandler;
    }
    private byte myReadByte(DataInputStream in) throws IOException
    {
    	byte[] c= new byte[1];
    	int bytesRead=0;
    	while ((bytesRead==0)&&!(Thread.currentThread().isInterrupted ())&&(!mChildHandler.hasMessages(STOP_VIDEO)))
    	{
    		bytesRead=in.read(c,0,1);
    		if(bytesRead<0)
    		{
    			throw new EOFException("MjpegInputStream::myReadFully: End of underlayer stream reached");
    		}
    	}
    	if(Thread.currentThread().isInterrupted ())
    		throw new InterruptedIOException();
    	if(bytesRead==0)
    	{
    		throw new IOException("VideoReaderThread::myReadByte: Unable to read byte");
    	}
    	return c[0];
    }
    
    
    
    private int getEndOfSeqeunce(DataInputStream in, byte[] sequence) throws IOException {
        int seqIndex = 0;
        byte c;
        for(int i=0; i < FRAME_MAX_LENGTH; i++) {
            c = myReadByte(in);
            if(c == sequence[seqIndex]) {
                seqIndex++;
                if(seqIndex == sequence.length) return i + 1;
            } else seqIndex = 0;
        }
        return -1;
    }
	
    private int getStartOfSequence(DataInputStream in, byte[] sequence) throws IOException {
        int end = getEndOfSeqeunce(in, sequence);
        return (end < 0) ? (-1) : (end - sequence.length);
    }

    private int parseContentLength(byte[] headerBytes, int headerLength) throws IOException, NumberFormatException {
        DataInputStream headerIn = new DataInputStream(new ByteArrayInputStream(headerBytes));
        int contentLengthEnd=getEndOfSeqeunce(headerIn,CONTENT_LENGTH);
        if(contentLengthEnd>0)
        {
        	int afterSemicolon=getEndOfSeqeunce(headerIn,COLON);
        	if(afterSemicolon>0)
        	{
        		headerIn.mark(headerLength);
        		int crLfPos=getEndOfSeqeunce(headerIn,EOL_MARKER);
        		headerIn.reset();
        		if(crLfPos>0)
        		{
        			
        			String length =  headerIn.readLine();
        			return Integer.parseInt(length);
        		}
        	}
        }

        return Integer.parseInt("huita"); // numberFormatException))
    }	
    
    byte[] buffer = new byte[FRAME_MAX_LENGTH];
    
    public Bitmap readMjpegFrame(DataInputStream in) throws IOException {
    	AVLogger.v("VideoReceiver", "recieving image");

    	in.mark(FRAME_MAX_LENGTH);
    	int boundary;
		try {
			
			
			boundary = getEndOfSeqeunce(in, BOUNDARY);
		} 
		catch (EOFException e) {
			
			throw e;
		}
		catch (IOException e) {
			
			e.printStackTrace();
			boundary =-1;
		}
    	if (boundary<0) return null;
    	in.reset();
    	in.skipBytes(boundary);
    	in.mark(FRAME_MAX_LENGTH);
        int headerLen = getStartOfSequence(in, SOI_MARKER);
        in.reset();
        //byte[] header = new byte[headerLen];
        myReadFully(in,buffer,0,headerLen);
        in.mark(JPEG_MAX_LENGTH);
        try {
            mContentLength = parseContentLength(buffer,headerLen);
        } catch (NumberFormatException nfe) { 
            mContentLength = getEndOfSeqeunce(in, EOF_MARKER); 
        }
        in.reset();
        
        //byte[] frameData = new byte[mContentLength];
        //skipBytes(headerLen);
        myReadFully(in,buffer,0,mContentLength);
        return BitmapFactory.decodeStream(new ByteArrayInputStream(buffer));
    }
    
    void myReadFully(InputStream in,byte[] dst, int offset, int byteCount) throws IOException
    {
    	int toRead=byteCount;
    	int curOffset=offset;
    	while ((toRead>0)&&!(Thread.currentThread().isInterrupted ())&&(!mChildHandler.hasMessages(STOP_VIDEO)))
    	{
    		int bytesRead=in.read(dst,curOffset,toRead);
    		if(bytesRead<0)
    		{
    			throw new EOFException("VideoReaderThread::myReadFully: End of underlayer stream reached");
    		}
    		else
    		{
    			curOffset+=bytesRead;
    			toRead-=bytesRead;
    		}
    	}
    	if(Thread.currentThread().isInterrupted ())
    		throw new InterruptedIOException();
    	if(toRead>0)
    	{
    		throw new IOException("VideoReaderThread::myReadFully: Unable to read all data");
    	}
    }
    
    
    
    
    


	public void setHostAndPort(String host, int port)
	{
		this.host = host;
		this.port = port;
	}
	
	
	public VideoReaderThread() {
		start();
		try {
			synchronized (sync) {
				sync.wait();
			}
		} catch (InterruptedException e) {

			AVLogger.e("", "", e);

		}
	}

	public void startWork() {
		isReading = true;
		internalStart();
	}

	protected void internalStart() {

		Message msg = mChildHandler.obtainMessage(START_VIDEO);
		mChildHandler.sendMessage(msg);
		AVLogger.v("avatar video in","internal start");
	}

	public void stopWork() {
		isReading = false;
		internalStop();
	}

	protected void internalStop() {
		
		Message msg = mChildHandler.obtainMessage(STOP_VIDEO);
		mChildHandler.removeMessages(START_VIDEO);
		mChildHandler.sendMessageAtFrontOfQueue(msg);
		AVLogger.v("avatar video in","internal stop");
	}

	Handler mChildHandler;

	private static final int START_VIDEO = 0;
	protected static final int PROCESS_VIDEO = 1;
	private static final int STOP_VIDEO = 2;
	protected static final int VIDEO_IN_ERROR = -3;
	protected static final int SAMPLE_RATE = 0;
	private volatile boolean isPlaying = false;
	private boolean isReading = false;

	public void run() {

		synchronized (sync) {
			Looper.prepare();
			setName("VideoReader");
			mChildHandler = new Handler() {

				Socket socket = null;
				DataInputStream videoStream;
				
				public void handleMessage(Message msg) {

					switch (msg.what) {
					case START_VIDEO:
						startPlay();
						break;
					case PROCESS_VIDEO:
						doPlay();
						break;
					case STOP_VIDEO:
						stopPlay();
						break;
					default:
						throw new RuntimeException("Unknown command to video writer thread");
					};

				}

				private void startPlay() {
					
					if (hasMessages(START_VIDEO)) {
						removeMessages(START_VIDEO);
					}
					if(isPlaying)
					{
						return;
					}
					
					
					AVLogger.v("avatar video in","starting play");
					closeSocket();
					
					InetAddress addr = null;
					try {
						addr = InetAddress.getByName(host);
					} catch (Exception e) {
						AVLogger.e("", "", e);
						errorHandler.sendMessageDelayed(errorHandler.obtainMessage(VIDEO_IN_ERROR),STD_DELAY);
						return;
					}

					try {
						socket = new Socket(addr, port);
						socket.setKeepAlive(true);
						socket.setSoTimeout(2000);
						
						OutputStream s = socket.getOutputStream();
						String ident =  getToken();
						//TODO - fix
						String header = 	  String.format(
											  "GET /restreamer?oid=%s&imagetag=%s HTTP/1.1"
											  +eol +"Server: %s:%d"+eol
											  +"User-Agent: avatar/0.2"+eol  +eol ,
											 ident, tag, host, port);
						s.write(header.getBytes());
						videoStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
						String dataToGet=(eol+eol);
						String data="";
						while (!(data.contains(dataToGet)||(data.length()>=1000)||interrupted()||hasMessages(STOP_VIDEO)))
						{
							data+=(char)videoStream.readByte();
						}
						if(!data.contains(dataToGet))
						{
							throw new IOException("Incorrect answer to http request!!!");
						}
						else
						{
							int boundaryMarkerPos = data.lastIndexOf(BOUNDARY_HEADER);
							
							if(boundaryMarkerPos>0)
							{
								boundaryMarkerPos+=BOUNDARY_HEADER.length();
								int crlfpos=data.indexOf(eol, boundaryMarkerPos);
								BOUNDARY=("--"+data.substring(boundaryMarkerPos, crlfpos)).getBytes();
							}
						}
						
					} catch (IOException e) {
						closeSocket();
						errorHandler.sendMessageDelayed(errorHandler.obtainMessage(VIDEO_IN_ERROR),STD_DELAY);
						return;
					}

					isPlaying = true;
					OnScreenLogger.setVideoIn(true);

					mChildHandler.obtainMessage(PROCESS_VIDEO).sendToTarget();
					
				}

				private void stopPlay() {
					
					AVLogger.v("avatar video in","stopping play");
					mChildHandler.removeMessages(PROCESS_VIDEO);
					mChildHandler.removeMessages(STOP_VIDEO);
					

					closeSocket();
					
				}

				private void closeSocket()
				{
					try {
						if (socket != null)
							socket.close();
					} catch (IOException e) {
						AVLogger.w("avatar video in", "error closing socket", e);
					}
					socket = null;
					videoStream= null;
					isPlaying = false;
					drawerHandler.obtainMessage(VideoDrawerThread.DONE).sendToTarget();
					OnScreenLogger.setVideoIn(false);
				}
				
				private void doPlay() {
					mChildHandler.removeMessages(PROCESS_VIDEO);
					if (isPlaying) {
							
							
						try {
							AVLogger.d("avatar video in","reading frame" );
							Bitmap b = readMjpegFrame(videoStream);
							if(b!=null)
							{
								AVLogger.d("avatar video in","reading frame succeed" );
								drawerHandler.removeMessages(VideoDrawerThread.RUN);
								drawerHandler.obtainMessage(VideoDrawerThread.RUN, b).sendToTarget();
							}
						} catch (IOException e) {
							AVLogger.w("avatar video in","reading frame failed" );
							closeSocket();
							errorHandler.sendMessageDelayed(errorHandler.obtainMessage(VIDEO_IN_ERROR),STD_DELAY);
						}

							
							if(isPlaying)
							{
								mChildHandler.obtainMessage(PROCESS_VIDEO).sendToTarget();
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

	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}

	private Handler errorHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case VIDEO_IN_ERROR:
				errorHandler.removeMessages(VIDEO_IN_ERROR);
				if (isReading) {
					mChildHandler.removeMessages(START_VIDEO);
					AVLogger.v("avatar video in","reconnecting on error");
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
