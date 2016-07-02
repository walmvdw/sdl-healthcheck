/*
 * Copyright 2016 Mark van der Wal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markwal.sdl.healthcheck;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import com.markwal.sdl.healthcheck.config.Configuration;
import com.markwal.sdl.healthcheck.config.ServiceNotFoundException;


@RestController
public class HealthCheckController {
	
	private static final Logger LOG = LoggerFactory.getLogger(HealthCheckController.class);
	
	@Autowired
	private Configuration config;
	
	private Map<String, ServiceConnection> connections = new HashMap<String, ServiceConnection>();
			
    @RequestMapping(value="/status/{serviceName}", produces="application/json")
    public @ResponseBody ResponseEntity<ServiceStatus> status(@PathVariable String serviceName) {
    	if (LOG.isInfoEnabled()) {
    		LOG.info("Request /status for service " + serviceName);
    	}
    	
    	ServiceConnection checker = this.getServiceConnection(serviceName);
    	
    	ServiceStatus status = checker.checkStatus();
    	if (status.getServiceStatus().equalsIgnoreCase("ok")) {
        	if (LOG.isInfoEnabled()) {
        		LOG.info("Successful health check result for " + serviceName);
        	}
    		return new ResponseEntity<ServiceStatus>(status, HttpStatus.OK);
    	} else {
    		LOG.warn("Failed health check result for " + serviceName + ": " + status.getStatusMessage());
    		return new ResponseEntity<ServiceStatus>(status, HttpStatus.INTERNAL_SERVER_ERROR);
    	}
    }

    private ServiceConnection getServiceConnection(String serviceName) {
    	if (LOG.isInfoEnabled()) {
    		LOG.info("Looking up connection to service: " + serviceName);
    	}
    	
    	ServiceConnection conn = this.connections.get(serviceName);
    	if (conn == null) {
        	if (LOG.isInfoEnabled()) {
        		LOG.info("Creating new connection for service: " + serviceName);
        	}
        	
    		conn = new ServiceConnection(this.config.getServiceInfo(serviceName));
    		this.connections.put(serviceName, conn);
    	}
    	
    	return conn;
	}

	@ExceptionHandler(ServiceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public @ResponseBody ErrorResponse handleServiceNotFoundException(Exception exc) {
		LOG.warn("ServiceNotFoundException", exc);
    	return new ErrorResponse("ServiceNotFoundException: " + exc.getMessage());
    }

    @ExceptionHandler(TokenException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody ErrorResponse handleTokenException(Exception exc) {
		LOG.warn("TokenException", exc);
    	return new ErrorResponse("TokenException: " + exc.getMessage());
    }

}