package com.markwal.sdl.healthcheck;

public class ServiceStatus {

	private String serviceName;
	private String serviceStatus;
	private String statusMessage;
	
	public ServiceStatus(String serviceName, String serviceStatus, String statusMessage) {
		this.serviceName = serviceName;
		this.serviceStatus = serviceStatus;
		this.statusMessage = statusMessage;
	}
	
	public String getServiceName() {
		return this.serviceName;
	}
	
	public String getServiceStatus() {
		return this.serviceStatus;
	}
	
	public String getStatusMessage() {
		return this.statusMessage;
	}
	
}
