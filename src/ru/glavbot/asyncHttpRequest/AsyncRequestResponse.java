package ru.glavbot.asyncHttpRequest;

public class AsyncRequestResponse {
	private int status;
	private Object data;
	private Throwable error;
	
	public static final int STATUS_PROGRESS=0;
	public static final int STATUS_INTERNAL_ERROR=600;
	
	
	public AsyncRequestResponse (int status, Object data, Throwable error)
	{
		this.status=status;
		this.error=error;
		this.data=data;
	}

	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public Throwable getError() {
		return error;
	}

	public void setError(Throwable error) {
		this.error = error;
	}
}
