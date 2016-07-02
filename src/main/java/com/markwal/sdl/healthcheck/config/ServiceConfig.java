package com.markwal.sdl.healthcheck.config;

public class ServiceConfig {

	private String name;
	private String protocol;
	private String host;
	private Integer port;
	private String uri;
	private String tokenUrl;
	private String clientId;
	private String clientSecret;
	
	public String getName() {
		return this.name;
	}
	
	public String getProtocol() {
		return this.protocol;
	}
	
	public String getHost() {
		return this.host;
	}
	
	public Integer getPort() {
		return this.port;
	}
	
	public String getUri() {
		return this.uri;
	}
	
	public String getTokenUrl() {
		return this.tokenUrl;
	}

	public String getClientId() {
		return this.clientId;
	}

	public String getClientSecret() {
		return this.clientSecret;
	}
	
}
