package com.markwal.sdl.healthcheck;

public class ErrorResponse {

	private String error;
	
	public ErrorResponse(String error) {
		this.error = error;
	}
	
	public String getErrorMessage() {
		return this.error;
	}
}
