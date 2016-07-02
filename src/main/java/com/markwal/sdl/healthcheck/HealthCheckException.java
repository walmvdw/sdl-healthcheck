package com.markwal.sdl.healthcheck;

@SuppressWarnings("serial")
public class HealthCheckException extends RuntimeException {

	public HealthCheckException(String message) {
		super(message);
	}

	public HealthCheckException(String message, Throwable exception) {
		super(message, exception);
	}

	public HealthCheckException(Throwable exception) {
		super(exception);
	}

}
