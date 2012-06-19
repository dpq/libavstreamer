package ru.glavbot.asyncHttpRequest;

public abstract class ProcessAsyncRequestResponceProrotype implements IProcessAsyncRequestResponse {

	public void processAsyncRequestResponse(AsyncRequestResponse resp) {
		// TODO Auto-generated method stub
		switch(resp.getStatus())
		{
		case AsyncRequestResponse.STATUS_INTERNAL_ERROR:
		{
			onConnectionFail(resp.getError());
			break;
			
		}
		case AsyncRequestResponse.STATUS_PROGRESS:
		{
			onDataPart(resp.getData());
			break;
			
		}
		default:
		{
			if ((resp.getStatus() >= 200) && (resp.getStatus() < 300)) {
				onConnectionSuccessful(resp.getData());
			} else {
				onConnectionUnsuccessful(resp.getStatus());
			}
		}
		};
	}

	protected abstract void onConnectionUnsuccessful(int status);

	protected void onConnectionSuccessful(Object data) {
		// TODO Auto-generated method stub
		
	}
	protected  void onDataPart(Object responce)
	{
		
	}

	protected abstract void onConnectionFail(Throwable e);
}
