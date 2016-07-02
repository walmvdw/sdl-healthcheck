package com.markwal.sdl.healthcheck;

import com.google.gson.annotations.SerializedName;

public class OAuthToken {

	@SerializedName("access_token")
	private String accessToken;
	
	@SerializedName("client_id")
	private String clientId;
	
	@SerializedName("refresh_token")
	private String refreshToken;
	
	@SerializedName("token_type")
	private String tokenType;
	
	@SerializedName("expires_in")
	private Long expiresIn;

	private long expireTime;
	
	public String getAccessToken() {
		return accessToken;
	}

	public String getClientId() {
		return clientId;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public Long getExpiresIn() {
		return expiresIn;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	public boolean isExpired() {
		if (System.currentTimeMillis() > this.expireTime) {
			return true;
		}
		return false;
	}
}
