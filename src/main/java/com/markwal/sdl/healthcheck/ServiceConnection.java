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

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.Gson;
import com.markwal.sdl.healthcheck.config.ServiceConfig;

public class ServiceConnection {

	private static final Pattern ERROR_PATTERN = Pattern
			.compile("\\{.*\"error\":.*");

	private ServiceConfig serviceConfig;
	private OAuthToken token;
	private Object checkerLock = new Object();

	public ServiceConnection(ServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	public ServiceStatus checkStatus() {
		ServiceStatus status;

		synchronized (checkerLock) {
			this.checkToken();

			try (CloseableHttpClient client = this.createClient();) {
				HttpResponse response = this.executeCheckRequest(client);
				String responseString = this.readResponse(response);

				if (response.getStatusLine().getStatusCode() == 401) {
					// authentication issue, request new token and try again
					this.token = null;
					this.requestToken();
					response = this.executeCheckRequest(client);
				}

				if (response.getStatusLine().getStatusCode() == 200) {
					status = new ServiceStatus(this.serviceConfig.getName(),
							"ok", "ok");
				} else {
					status = new ServiceStatus(
							this.serviceConfig.getName(),
							"error-" + response.getStatusLine().getStatusCode(),
							responseString);
				}
			} catch (IOException e) {
				throw new HealthCheckException(e);
			}
		}

		return status;
	}

	private HttpResponse executeCheckRequest(HttpClient client)
			throws ClientProtocolException, IOException {
		String url = this.serviceConfig.getProtocol() + "://"
				+ this.serviceConfig.getHost() + ":"
				+ this.serviceConfig.getPort() + "/"
				+ this.serviceConfig.getUri();
		HttpGet request = new HttpGet(url);
		request.setConfig(this.createRequestConfig());
		request.addHeader("authorization",
				"Bearer " + this.token.getAccessToken());

		HttpResponse response = client.execute(request);
		return response;
	}

	private void checkToken() {

		if ((this.token == null) || (token.isExpired())) {
			this.requestToken();
		}
	}

	private RequestConfig createRequestConfig() {
		RequestConfig config = RequestConfig.custom()
				.setConnectionRequestTimeout(1000).setConnectTimeout(1000)
				.setSocketTimeout(1000).build();
		return config;
	}

	private CloseableHttpClient createClient() {
		HttpClientBuilder builder = HttpClientBuilder.create();

		// Use system properties if specified (e.g. for proxy)
		builder.useSystemProperties();
		CloseableHttpClient client = builder.build();

		return client;
	}

	private void requestToken() {
		HttpPost tokenRequest = new HttpPost(this.serviceConfig.getTokenUrl());
		tokenRequest.addHeader("Accept", "application/json");
		tokenRequest.addHeader("Content-Type", "application/json");

		tokenRequest.setConfig(this.createRequestConfig());

		String requestBody = "grant_type=client_credentials&client_id="
				+ this.serviceConfig.getClientId() + "&client_secret="
				+ this.serviceConfig.getClientSecret();

		HttpResponse response;
		try (CloseableHttpClient client = this.createClient()) {
			HttpEntity entity = new ByteArrayEntity(requestBody.getBytes("UTF-8"));
			tokenRequest.setEntity(entity);

			response = client.execute(tokenRequest);

			String responseString = this.readResponse(response);
			if (this.isErrorResponse(responseString)) {
				this.token = null;
				throw new TokenException(this.getTokenError(responseString));
			} else {
				this.token = this.parseResponseToken(responseString);
			}
		} catch (IOException e) {
			throw new HealthCheckException(e);
		}


	}

	private OAuthToken parseResponseToken(String responseString) {
		Gson gson = new Gson();

		OAuthToken tokenResponse = gson.fromJson(responseString,
				OAuthToken.class);
		// subtract 500 ms to allow for duration of request.
		tokenResponse.setExpireTime(System.currentTimeMillis()
				+ tokenResponse.getExpiresIn() - 500);
		return tokenResponse;
	}

	private String getTokenError(String responseString) {
		Gson gson = new Gson();

		ErrorResponse error = gson
				.fromJson(responseString, ErrorResponse.class);
		return error.getErrorMessage();
	}

	private boolean isErrorResponse(String responseString) {
		Matcher errorMatcher = ERROR_PATTERN.matcher(responseString);
		return errorMatcher.matches();
	}

	private String readResponse(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		String result;

		try {
			InputStream contentStream = entity.getContent();
			result = IOUtils.toString(contentStream, "UTF-8");
		} catch (IOException e) {
			throw new HealthCheckException(e);
		}

		if (result != null) {
			result = result.trim();
		}
		return result;
	}

}
