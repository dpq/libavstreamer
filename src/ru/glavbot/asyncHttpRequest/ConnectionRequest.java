package ru.glavbot.asyncHttpRequest;



public class ConnectionRequest  {



		public static final int GET = 0;
		public static final int POST = 1;
		public static final int PUT = 2;
		public static final int DELETE = 3;

		public static final int READ_ALL = 0;
		public static final int READ_STRINGS_ONE_BY_ONE = 1;
		public static final int RETURN_REQUEST_ENTITY = 2;
		
		
		
		
		private String url;
		private int method;
		private String data;

		private int processingType=0;
		
		private int timeout = 100000;
		private IProcessAsyncRequestResponse progressProcessor=null;
		private IProcessAsyncRequestResponse answerProcessor=null;
	
		public  ConnectionRequest(int method, String url) {
			this.method=method;
			this.url=url;
			this.data="";
		
		}
		
	
		public  ConnectionRequest(int method, String url, String data) {
			this.method=method;
			this.url=url;
			this.data=data;
		}

	


		public int getTimeout() {
			return timeout;
		}

		public void setTimeout(int timeout) {
			this.timeout = timeout;
		}



		public String getUrl() {
			return url;
		}




		public void setUrl(String url) {
			this.url = url;
		}




		public int getMethod() {
			return method;
		}




		public void setMethod(int method) {
			this.method = method;
		}




		public String getData() {
			return data;
		}




		public void setData(String data) {
			this.data = data;
		}


		public IProcessAsyncRequestResponse getProgressProcessor() {
			return progressProcessor;
		}


		public void setProgressProcessor(IProcessAsyncRequestResponse responceProcessor) {
			this.progressProcessor = responceProcessor;
		}


		public IProcessAsyncRequestResponse getAnswerProcessor() {
			return answerProcessor;
		}


		public void setAnswerProcessor(IProcessAsyncRequestResponse answerProcessor) {
			this.answerProcessor = answerProcessor;
		}


		public int getProcessingType() {
			return processingType;
		}


		public void setProcessingType(int processingType) {
			this.processingType = processingType;
		}



	
}
