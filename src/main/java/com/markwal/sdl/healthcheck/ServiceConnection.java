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

import com.google.gson.Gson;
import com.markwal.sdl.healthcheck.config.EnvVarSubstitutor;
import com.markwal.sdl.healthcheck.config.ServiceConfig;
import com.tridion.crypto.Crypto;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceConnection {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceConnection.class);

    private static final Pattern ERROR_PATTERN = Pattern.compile("\\{.*\"error\":.*");
    private final Object checkerLock = new Object();
    private final EnvVarSubstitutor envVarSubstitutor = new EnvVarSubstitutor();
    private ServiceConfig serviceConfig;
    private OAuthToken token;

    public ServiceConnection(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public ServiceStatus checkStatus() {
        ServiceStatus status;

        if (LOG.isInfoEnabled()) {
            LOG.info("Checking status for service '" + serviceConfig.getName() + "'");
        }

        synchronized (checkerLock) {
            this.checkToken();

            try (CloseableHttpClient client = this.createClient()) {

                HttpResponse response = this.executeCheckRequest(client);
                String responseString = this.readResponse(response);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Response: " + responseString);
                }

                if (response.getStatusLine().getStatusCode() == 401) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Received 401 error, refreshing token and trying again");
                    }
                    // authentication issue, request new token and try again
                    this.token = null;
                    this.requestToken();
                    response = this.executeCheckRequest(client);
                }

                if (LOG.isInfoEnabled()) {
                    LOG.info("Received status code: " + response.getStatusLine().getStatusCode());
                    LOG.info("Received status message: " + response.getStatusLine().getReasonPhrase());
                }

                if (response.getStatusLine().getStatusCode() == 200) {
                    status = new ServiceStatus(this.serviceConfig.getName(),
                            "ok", "ok");
                } else {
                    LOG.warn("Received error status code: " + response.getStatusLine().getStatusCode());
                    LOG.warn("Received error status message: " + response.getStatusLine().getReasonPhrase());

                    status = new ServiceStatus(
                            this.serviceConfig.getName(),
                            "error-" + response.getStatusLine().getStatusCode(),
                            responseString);
                }
            } catch (HttpHostConnectException e) {
                LOG.warn("Connect exception: " + e.getMessage(), e);
                status = new ServiceStatus(this.serviceConfig.getName(), "error-connect", e.getMessage());
            } catch (IOException e) {
                throw new HealthCheckException(e);
            }
        }

        return status;
    }

    private HttpResponse executeCheckRequest(HttpClient client)
            throws IOException {

        String url = envVarSubstitutor.replace(this.serviceConfig.getProtocol()) + "://"
                + envVarSubstitutor.replace(this.serviceConfig.getHost()) + ":"
                + envVarSubstitutor.replace(this.serviceConfig.getPort()) + "/"
                + envVarSubstitutor.replace(this.serviceConfig.getUri());

        if (LOG.isTraceEnabled()) {
            LOG.trace("Check URL: " + url);
        }

        HttpGet request = new HttpGet(url);
        request.setConfig(this.createRequestConfig());
        request.addHeader("authorization",
                "Bearer " + this.token.getAccessToken());

        if (LOG.isInfoEnabled()) {
            LOG.info("Executing check request: " + url);
        }

        HttpResponse response = client.execute(request);

        return response;
    }

    private void checkToken() {
        if ((this.token == null) || (token.isExpired())) {
            if (LOG.isInfoEnabled()) {
                LOG.info("No token or token is expired, requesting new token");
            }
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
        String tokenUrl = envVarSubstitutor.replace(this.serviceConfig.getTokenUrl());

        if (LOG.isInfoEnabled()) {
            LOG.info("Requesting token from: " + tokenUrl);
        }

        HttpPost tokenRequest = new HttpPost(tokenUrl);
        tokenRequest.addHeader("Accept", "application/json");
        tokenRequest.addHeader("Content-Type", "application/json");

        //tokenRequest.setConfig(this.createRequestConfig());

        String requestBody = "grant_type=client_credentials&client_id="
                + envVarSubstitutor.replace(this.serviceConfig.getClientId()) + "&client_secret="
                + this.decryptIfNeeded(envVarSubstitutor.replace(this.serviceConfig.getClientSecret()));

        HttpResponse response;
        try (CloseableHttpClient client = this.createClient()) {
            HttpEntity entity = new ByteArrayEntity(requestBody.getBytes("UTF-8"));
            tokenRequest.setEntity(entity);

            response = client.execute(tokenRequest);
            if (LOG.isInfoEnabled()) {
                LOG.info("Received status code: " + response.getStatusLine().getStatusCode());
                LOG.info("Received status message: " + response.getStatusLine().getReasonPhrase());
            }

            String responseString = this.readResponse(response);
            if (this.isErrorResponse(responseString)) {
                LOG.warn("Received error from Token service: " + responseString);
                this.token = null;
                throw new TokenException(this.getTokenError(responseString));
            } else {
                this.token = this.parseResponseToken(responseString);
                if (LOG.isInfoEnabled()) {
                    LOG.info("Successfully retrieved a token");
                }
            }
        } catch (IOException e) {
            LOG.warn("IOException while retrieving token: " + e.getMessage());
            throw new HealthCheckException(e);
        }


    }

    private String decryptIfNeeded(String clientSecret) {
        String result;

        try {
            result = Crypto.decryptIfNecessary(clientSecret);
        } catch (GeneralSecurityException e) {
            throw new HealthCheckException("Error decrypting client secret: " + e.getMessage());
        }

        return result;
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
            LOG.warn("Error while parsing response: " + e.getMessage());
            throw new HealthCheckException(e);
        }

        if (result != null) {
            result = result.trim();
        }
        return result;
    }

}
