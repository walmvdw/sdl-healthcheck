package com.markwal.sdl.healthcheck.config;

import com.markwal.sdl.healthcheck.HealthCheckException;

@SuppressWarnings("serial")
public class ServiceNotFoundException extends HealthCheckException {

	public ServiceNotFoundException(String serviceName) {
		super("Service '" + serviceName + "' not found");
	}

}
