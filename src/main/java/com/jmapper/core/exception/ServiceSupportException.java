package com.jmapper.core.exception;

public class ServiceSupportException extends RuntimeException{

	public ServiceSupportException(String string) {
		super(string);
	}

	public ServiceSupportException() {
		super();
		
	}

	public ServiceSupportException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		
	}

	public ServiceSupportException(Throwable arg0) {
		super(arg0);
		
	}

}
