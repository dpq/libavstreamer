package ru.glavbot.AVRestreamer;




//import java.util.Date;


public class HandlerBuffer {

	private byte[] data;
	//private DataInputStream ds;
	//Date d;
	private volatile boolean locked;
	HandlerBuffer(int size)
	{
		data=new byte[size];
		locked=false;
	//	ds = new DataInputStream(new ByteArrayInputStream(data));
	//	ds.mark(data.length);
	}
	public synchronized boolean isLocked() {
		return locked;
	}
	public synchronized void lock() {
		this.locked = true;
	}
	public synchronized void unlock() {
		this.locked = false;
	}
	public final byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		
		 System.arraycopy(data, 0,this.data , 0, data.length);
		 
		/* try{
			 ds.reset();
		 } catch (IOException e){}*/
	}
	
	/*public DataInputStream getDataStream()
	{
		return ds;
	}*/
}
