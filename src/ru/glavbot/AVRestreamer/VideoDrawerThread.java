package ru.glavbot.AVRestreamer;


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
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoDrawerThread extends Thread {
   	protected static final int INIT = 0;
	protected static final int RUN = 1;
	protected static final int DONE = 2;
	
	Object sync = new Object();
    public final static int POSITION_UPPER_LEFT  = 9;
    public final static int POSITION_UPPER_RIGHT = 3;
    public final static int POSITION_LOWER_LEFT  = 12;
    public final static int POSITION_LOWER_RIGHT = 6;

    public final static int VIDEO_IN_ERROR=-1;

    private boolean surfaceDone = false;   
	
    private Paint overlayPaint;
    private int overlayTextColor = Color.WHITE;
    private int overlayBackgroundColor = Color.BLACK;
    private int ovlPos=POSITION_UPPER_RIGHT;

    private volatile SurfaceHolder mSurfaceHolder;
    
    
	
	private Handler mChildHandler;
    private Bitmap ovl;
    private int frameCounter = 0;
   	 
    public VideoDrawerThread(SurfaceView surface)
    {
    	super();
    	//this.surface=surface;
    	start();
		try {
			synchronized (sync) {
				sync.wait();
			}
		} catch (InterruptedException e) {

			Log.e("", "", e);
		}
    	mSurfaceHolder=surface.getHolder();
    	mSurfaceHolder.addCallback(shc);
    	//if(mSurfaceHolder.)
        overlayPaint = new Paint();
        overlayPaint.setTextAlign(Paint.Align.LEFT);
        overlayPaint.setTextSize(12);
        overlayPaint.setTypeface(Typeface.DEFAULT);
    }
    
    int surfaceWidth=0;
    int surfaceHeight=0;
    
    SurfaceHolder.Callback shc= new SurfaceHolder.Callback(){

		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
	    	mSurfaceHolder=holder;
	    	surfaceWidth= width;
	    	surfaceHeight=height;
	    }

	    public void surfaceDestroyed(SurfaceHolder holder) { 
	        surfaceDone = false; 
	        stopPlayback(); 
	    }

		public void surfaceCreated(SurfaceHolder holder) {
			surfaceDone = true; 
			startPlayback(); 
			
		}
    };
    
   
    protected void startPlayback() {
		// TODO Auto-generated method stub
    	mChildHandler.obtainMessage(INIT).sendToTarget();
    	
		
	}
	protected void stopPlayback() {
		// TODO Auto-generated method stub
		mChildHandler.obtainMessage(DONE).sendToTarget();
	}

    public void setOverlayTextColor(int c) { overlayTextColor = c; }
    public void setOverlayBackgroundColor(int c) { overlayBackgroundColor = c; }
    public void setOverlayPosition(int p) { ovlPos = p; }
     
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
    }

	 @Override
	 public void run() {
		 synchronized (sync) {
		 	Looper.prepare();
	   	    this.setName("VideoDrawerThread");    
	   	    mChildHandler=new Handler() {
   		        long start;
   		        PorterDuffXfermode mode=null;
   		        Paint p=null;
   		        private String fps;
	   	        	
   		        public void handleMessage(Message msg) {
	   		        switch (msg.what){
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
	   			   
	   				if(mode==null)
	   					mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
	   			    
	   			    if(p==null)
	   			    	p = new Paint();
	   			    fps = "";
	   			    frameCounter = 0;
	   			}

	   			private void doRun(Bitmap frame) 
	   			{
	   				Log.v("VideoReceiver", "drawing image");
	   				if(surfaceDone) {
	   		            Canvas c=null;
	   					try {
	   		                c = mSurfaceHolder.lockCanvas();
	   		                synchronized (mSurfaceHolder) {
	   		                    c.drawColor(Color.BLACK);
	   		                    if(frame!=null)
	   		                    {
	   		                        Rect destRect = new Rect(0, 0, surfaceWidth, surfaceHeight);
		   		                    c.drawBitmap(frame, null, destRect, p);
		   		                    frame.recycle();
		   		                           
		   		                   
		   		                    if(ovl != null) {
		   		                    	p.setXfermode(mode);
		   		                        int height = ((ovlPos & 1) == 1) ? destRect.top : destRect.bottom-ovl.getHeight();
		   		                        int width  = ((ovlPos & 8) == 8) ? destRect.left : destRect.right -ovl.getWidth();
		   		                        c.drawBitmap(ovl, width, height, null);
		   		                        p.setXfermode(null);
		   		                    }
		   		                    
		   		                    frameCounter++;
		   		                    if((System.currentTimeMillis() - start) >= 1000) {
		   		                        fps = String.valueOf(frameCounter)+"fps";
		   		                        frameCounter = 0; 
		   		                        start = System.currentTimeMillis();
	   		                            makeFpsOverlay(overlayPaint, fps);
	   		                        }
	   		                    }
	   		                }
	   		            } finally { 
	   		            	if (c != null) mSurfaceHolder.unlockCanvasAndPost(c); 
	   		            }
	   				}
	   			}

	   			private void done() {
	   				int [] img = new int[1];
	   				img[0]=0;
	   				//Bitmap.createBitmap(img, 1, 1, null);
	   				
	   				doRun(Bitmap.createBitmap(img, 1, 1, Bitmap.Config.RGB_565));
	   			}
	   		};
	   		        
	   		sync.notifyAll();
		}
		Looper.loop();
	}// EO run()

	public Handler getChildHandler() {
	   	return mChildHandler;
	}   
}
