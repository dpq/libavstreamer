package de.mjpegsample.MjpegView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import ru.glavbot.asyncHttpRequest.ConnectionManager;
import ru.glavbot.asyncHttpRequest.ConnectionRequest;
import ru.glavbot.asyncHttpRequest.ProcessAsyncRequestResponceProrotype;



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MjpegView extends SurfaceView implements SurfaceHolder.Callback {
    public final static int POSITION_UPPER_LEFT  = 9;
    public final static int POSITION_UPPER_RIGHT = 3;
    public final static int POSITION_LOWER_LEFT  = 12;
    public final static int POSITION_LOWER_RIGHT = 6;

    public final static int SIZE_STANDARD   = 1; 
    public final static int SIZE_BEST_FIT   = 4;
    public final static int SIZE_FULLSCREEN = 8;
    public final static int VIDEO_IN_ERROR=-1;
    
    private MjpegViewThread thread;
    private MjpegInputStream mIn = null;    
    private boolean showFps = false;
    private boolean mRun = false;
    private boolean surfaceDone = false;    
    private Paint overlayPaint;
    private int overlayTextColor;
    private int overlayBackgroundColor;
    private int ovlPos;
    private volatile int dispWidth;
    private volatile int dispHeight;
    private volatile SurfaceHolder mSurfaceHolder;
    
    private int displayMode;
    private int frameCounter = 0;

    
    private Handler errorHandler = new Handler()
    {
    	@Override
    	 public void handleMessage(Message msg) {
            	
            	switch (msg.what)
            	{
            		case VIDEO_IN_ERROR:
            			if(mRun)
            			{
            				stopPlayback();
            				requestRead(url);
            			}
            			break;
            		default:
            			throw new RuntimeException("Unknown command to incoming video error handler");
            	};
				
            }
    };
    
    
    
    
    
    PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
    //Bitmap bm;
    int width;
    int height;
    Rect destRect;
    Canvas c = null;
    Paint p = new Paint();
    String fps = "";
    
	public class MjpegViewThread extends Thread {
		public void run() {
			try {
				setName("VideoReceiver");
				while (mRun && (!interrupted())) {

					Bitmap bm = mIn.readMjpegFrame();
					if(bm!=null)
					{
						Handler h = drawerThread.getChildHandler();
						h.removeMessages(DrawerThread.RUN);
						h.obtainMessage(DrawerThread.RUN, bm).sendToTarget();
					}
					// System.gc();
				}
			} catch (IOException e) {
				
					if(mIn!=null)
					{
						mIn.close();
						mIn = null;
					}
				
				errorHandler.obtainMessage(VIDEO_IN_ERROR, e).sendToTarget();
				Log.e("", "", e);
			}
		}
	}

    private void init(Context context) {
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
      //  this.context=context;
        
        setFocusable(true);
        overlayPaint = new Paint();
        overlayPaint.setTextAlign(Paint.Align.LEFT);
        overlayPaint.setTextSize(12);
        overlayPaint.setTypeface(Typeface.DEFAULT);
        overlayTextColor = Color.WHITE;
        overlayBackgroundColor = Color.BLACK;
        ovlPos = MjpegView.POSITION_UPPER_RIGHT;
        displayMode = MjpegView.SIZE_STANDARD;
        dispWidth = getWidth();
        dispHeight = getHeight();
        mSurfaceHolder=getHolder();
        drawerThread = new DrawerThread();
        drawerThread.start();
        
    }
    
    public void startPlayback() { 
        if(mIn != null) {
            mRun = true;
            thread = new MjpegViewThread();
            if(drawerThread!=null)
            {
            	drawerThread.getChildHandler().obtainMessage(DrawerThread.INIT).sendToTarget();
            	//drawerThread.setSurfaceSize(surfaceWidth, surfaceHeight); 
            }
            thread.start();    		
        }
    }
    
    public void stopPlayback() { 
        mRun = false;
        initializing=false;
        boolean retry = true;
        
        while(retry) {
            try {
            	
            	if(thread!=null)
            	{
            		if(thread.isAlive())
            		{
            			thread.interrupt();
            			thread.join();
            		}
            		Log.d("",thread.isAlive()?"alive":"dead");
            		thread=null;
            	}
            	retry = false;
            } catch (InterruptedException e) {}
        }
        

        	if(mIn!=null)
			{
        		mIn.close();
        		mIn=null;
			}

        
        drawerThread.getChildHandler().obtainMessage(DrawerThread.DONE).sendToTarget();

    }

    public MjpegView(Context context, AttributeSet attrs) { super(context, attrs); init(context); }
    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) { 
    	//if(drawerThread !=null)
    	//{
    	//	drawerThread.setSurfaceSize(w, h); 
    	//}
    	//surfaceWidth=w;
    	//surfaceHeight=h;
    	mSurfaceHolder=holder;
    	dispWidth = w;
    	dispHeight = h;
    }

    public void surfaceDestroyed(SurfaceHolder holder) { 
        surfaceDone = false; 
        stopPlayback(); 
    }
    
    public MjpegView(Context context) { super(context); init(context); }    
    public void surfaceCreated(SurfaceHolder holder) { surfaceDone = true; }
    public void showFps(boolean b) { showFps = b; }
    private void setSource(MjpegInputStream source) { mIn = source; startPlayback();}
    public void setOverlayPaint(Paint p) { overlayPaint = p; }
    public void setOverlayTextColor(int c) { overlayTextColor = c; }
    public void setOverlayBackgroundColor(int c) { overlayBackgroundColor = c; }
    public void setOverlayPosition(int p) { ovlPos = p; }
    public void setDisplayMode(int s) { displayMode = s; }
    
    
    /*
    private static class MJpegInputStreamConnector implements Runnable
    {

    	MJpegInputStreamConnector(String url, ConnectionResponceHandler handler)
    	{
    		this.url=URI.create(url);
    		this.m_handler=handler.getNativeHandler();
    	};
    	 private URI url= null;
    	 private Handler m_handler=null;
    	 private HttpResponse res= null;

    	 private static DefaultHttpClient httpClient=null;
    	 
    	 private DefaultHttpClient getDefaultHttpClient()
    	 {
    		 if(httpClient==null)
    		 {
    			 httpClient = new DefaultHttpClient();
    		 }
    		 return httpClient;
    	 }
    	 
		public void run() {
			// TODO Auto-generated method stub

			if (url == null || m_handler == null)
				throw new RuntimeException(
						"MJpegInputStreamConnector started without thread or handler");

			DefaultHttpClient client= getDefaultHttpClient();
			//httpclient.
			Message message;
			try {
				res = client.execute(new HttpGet(url));
				AsyncRequestResponce rr = new AsyncRequestResponce(res.getStatusLine()
						.getStatusCode(), res.getEntity().getContent(),Thread.currentThread(),null);
				message = Message.obtain(m_handler, HttpConnection.DID_SUCCEED,
						rr);
			} catch (ClientProtocolException e) {
				Log.e("", "", e);
				message = Message
						.obtain(m_handler, HttpConnection.DID_ERROR, new AsyncRequestResponce(600, null,Thread.currentThread(),e));
				
			} catch (IOException e) {
				Log.e("", "", e);
				message = Message
						.obtain(m_handler, HttpConnection.DID_ERROR, new AsyncRequestResponce(600, null,Thread.currentThread(),e));
			}

			if(message.what!=HttpConnection.DID_SUCCEED)
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				Log.e("","",e1);
				
			}
			m_handler.sendMessage(message);

		}
    };
   
    */
    
    private String url;
    private boolean initializing=false;
    private ConnectionManager manager = new ConnectionManager();
    
    public void requestRead(String url) {
    	this.url=url;
    	initializing=true;
    	/*MJpegInputStreamConnector r = new MJpegInputStreamConnector(url,openStreamHandler);
    	Thread t = new Thread(r);
    	t.start();*/
		ConnectionRequest req= new ConnectionRequest(ConnectionRequest.GET, url);
		req.setProcessingType(ConnectionRequest.RETURN_REQUEST_ENTITY);
		req.setAnswerProcessor(openStreamHandler);
		manager.push(req);
    }
    
    
    
    ProcessAsyncRequestResponceProrotype openStreamHandler= new ProcessAsyncRequestResponceProrotype()
    {

		@Override
		protected void onConnectionSuccessful(Object responce) {
			// TODO Auto-generated method stub
			 try {
				setSource(new MjpegInputStream((HttpEntity) responce));
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				Log.e("","",e);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e("","",e);
				
			}
			 initializing=false;
			 //view.setSource();
			 setDisplayMode(MjpegView.SIZE_BEST_FIT);
			 showFps(true);
			 
		}


		@Override
		protected void onConnectionUnsuccessful(int statusCode) {
			// TODO Auto-generated method stub
			if(initializing)
				requestRead(url);
			
		}

		@Override
		protected void onConnectionFail(Throwable e) {
			// TODO Auto-generated method stub
			if(initializing)
				requestRead(url);
		}
    	
    };
    private DrawerThread drawerThread=null;
    
    
    private class DrawerThread extends Thread {
   	protected static final int INIT = 0;
	protected static final int RUN = 1;
	protected static final int DONE = 2;
	private Handler mChildHandler;
    private long start;
    private Bitmap ovl;
   	 
    public DrawerThread()
    {
    	super();
    	this.setName("DrawerThread");
    }
    
    
    
        private Rect destRect(int bmw, int bmh) {
            int tempx;
            int tempy;
            if (displayMode == MjpegView.SIZE_STANDARD) {
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == MjpegView.SIZE_BEST_FIT) {
                float bmasp = (float) bmw / (float) bmh;
                bmw = dispWidth;
                bmh = (int) (dispWidth / bmasp);
                if (bmh > dispHeight) {
                    bmh = dispHeight;
                    bmw = (int) (dispHeight * bmasp);
                }
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == MjpegView.SIZE_FULLSCREEN) return new Rect(0, 0, dispWidth, dispHeight);
            return null;
        }
         
         
        private void /*Bitmap*/ makeFpsOverlay(Paint p, String text) {
            Rect b = new Rect();
            p.getTextBounds(text, 0, text.length(), b);
            int bwidth  = b.width()+2;
            int bheight = b.height()+2;
            if(ovl==null||(ovl.getHeight()!=bheight)||(ovl.getWidth()!=bwidth))
            {
            	if(ovl!=null)
            	{
            		ovl.recycle();
            	}
            	ovl = Bitmap.createBitmap(bwidth, bheight, Bitmap.Config.ARGB_8888);
            }
            Canvas c = new Canvas(ovl);
            p.setColor(overlayBackgroundColor);
            c.drawRect(0, 0, bwidth, bheight, p);
            p.setColor(overlayTextColor);
            c.drawText(text, -b.left+1, (bheight/2)-((p.ascent()+p.descent())/2)+1, p);
          //  return bm;        	 
        }


   	 
   	 
   	 @Override
   	public void run() {
   	        Looper.prepare();
   		        
   	        mChildHandler=new Handler() {
   		            public void handleMessage(Message msg) {
   		            	
   		            	switch (msg.what)
   		            	{
   		            		case INIT:
   		            			init();
   		            			break;
   		            		case RUN:
   		            			doRun((Bitmap)msg.obj);
   		            			break;
   		            		case DONE:
   		            			done();
   		            			break;
   		            		default:
   		            			throw new RuntimeException("Unknown command to video drawer thread");
   		            	};
   						
   		            }

   					private void init() {
   						start = System.currentTimeMillis();
   			            mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
   			           // bm=null;
   			            c = null;
   			            p = new Paint();
   			            width=0;
   			            height=0;
   			            fps = "";
   			            frameCounter = 0;
   					}

   					private void doRun(Bitmap frame) 
   					{
   						Log.v("VideoReceiver", "drawing image");
   						if(surfaceDone) {
   		                    try {
   		                        c = mSurfaceHolder.lockCanvas();
   		                        synchronized (mSurfaceHolder) {
   		                                c.drawColor(Color.BLACK);
   		                                if(frame!=null)
   		                                {
	   		                                destRect = destRect(frame.getWidth(),frame.getHeight());
	   		                                c.drawBitmap(frame, null, destRect, p);
	   		                                frame.recycle();
	   		                                if(showFps) {
	   		                                    p.setXfermode(mode);
	   		                                    if(ovl != null) {
	   		                                    	height = ((ovlPos & 1) == 1) ? destRect.top : destRect.bottom-ovl.getHeight();
	   		                                    	width  = ((ovlPos & 8) == 8) ? destRect.left : destRect.right -ovl.getWidth();
	   		                                        c.drawBitmap(ovl, width, height, null);
	   		                                    }
	   		                                    p.setXfermode(null);
	   		                                    frameCounter++;
	   		                                    if((System.currentTimeMillis() - start) >= 1000) {
	   		                                        fps = String.valueOf(frameCounter)+"fps";
	   		                                        frameCounter = 0; 
	   		                                        start = System.currentTimeMillis();
   		                                        	makeFpsOverlay(overlayPaint, fps);
   		                                   		}
   		                                	}
   		                                }
   		                        }
   		                    } finally { if (c != null) mSurfaceHolder.unlockCanvasAndPost(c); }
   						}
   					}

   					private void done() {
   						doRun(null);
   					}


   		        };
   		        Looper.loop();
   		    }

   		public Handler getChildHandler() {
   			return mChildHandler;
   		}
   };
    
    
    
}