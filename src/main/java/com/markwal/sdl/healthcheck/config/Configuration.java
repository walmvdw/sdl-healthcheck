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

package com.markwal.sdl.healthcheck.config;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.markwal.sdl.healthcheck.HealthCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Component
@ConfigurationProperties()
public class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    @Value("${config.services.file}")
	private String servicesConfig;
	
	private Map<String, ServiceConfig> serviceMap;
	private Object serviceMapLock = new Object();

	public String getServicesConfig() {
		return this.servicesConfig;
	}

	public ServiceConfig getServiceInfo(String serviceName) {
		ServiceConfig service = this.getServices().get(serviceName);
		if (service == null) {
			throw new ServiceNotFoundException(serviceName);
		}
		
		return service;
	}
	
    private Map<String, ServiceConfig> getServices() {
    	if (serviceMap == null) {
    		synchronized (serviceMapLock) {
    	    	if (serviceMap == null) {
    	    		this.readServices();
    	    	}
    		}
    	}
    	
    	return this.serviceMap;
    	
    }
    
    private void readServices() {
        File servicesFile = this.findServicesFile();

        this.serviceMap = new HashMap<String, ServiceConfig>();
    	
    	try ( InputStreamReader reader = new FileReader(servicesFile) ) {
    	
	    	Type type = new TypeToken<List<ServiceConfig>>() {}.getType();
	    	
	    	Gson gson = new Gson();
	    	List<ServiceConfig> serviceList = gson.fromJson(reader, type);
	    	for (ServiceConfig serviceConfig : serviceList) {
	    		this.serviceMap.put(serviceConfig.getName(), serviceConfig);
	    	}
    	} catch (IOException e) {
    		throw new HealthCheckException(e);
		}

	}

    private File findServicesFile() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Checking services file: " + this.getServicesConfig());
        }

        File servicesFile = new File("./config/" + this.getServicesConfig());

        if (!servicesFile.exists()) {
            LOG.warn("Services file '"  +servicesFile.getAbsolutePath() + "' not found");
        }

        if (!servicesFile.isFile()) {
            LOG.warn("Services file '"  +servicesFile.getAbsolutePath() + "' is not a file");
        }

        if (!servicesFile.canRead()) {
            LOG.warn("Services file '"  +servicesFile.getAbsolutePath() + "' is not readable");
        }

        return servicesFile;
    }


}