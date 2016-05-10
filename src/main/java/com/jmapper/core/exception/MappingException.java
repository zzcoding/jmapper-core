package com.jmapper.core.exception;

public class MappingException extends RuntimeException{

	public MappingException(String string) {
		super(string);
	}

	public MappingException() {
		super();
		
	}

	public MappingException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		
	}

	public MappingException(Throwable arg0) {
		super(arg0);
		
	}

}
