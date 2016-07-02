package com.markwal.sdl.healthcheck.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Component
@ConfigurationProperties()
public class Configuration {

	@Value("${checks.config.path}")
	private String configPath;
	
	private Map<String, ServiceConfig> serviceMap;
	private Object serviceMapLock = new Object();
	
	public String getConfigPath() {
		return this.configPath;
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
    	this.serviceMap = new HashMap<String, ServiceConfig>();
    	
    	try ( InputStreamReader reader = new InputStreamReader(Configuration.class.getResourceAsStream(this.getConfigPath())) ) {
    	
	    	Type type = new TypeToken<List<ServiceConfig>>() {}.getType();
	    	
	    	Gson gson = new Gson();
	    	List<ServiceConfig> serviceList = gson.fromJson(reader, type);
	    	for (ServiceConfig serviceConfig : serviceList) {
	    		this.serviceMap.put(serviceConfig.getName(), serviceConfig);
	    	}
    	} catch (IOException e) {
    		throw new RuntimeException(e);
		}

	}

	
}