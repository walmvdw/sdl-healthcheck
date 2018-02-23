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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.markwal.sdl.healthcheck.HealthCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ConfigurationProperties()
public class Configuration {

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    private final Object serviceMapLock = new Object();

    @Value("${config.services.file}")
    private String servicesConfig;

    @Value("${config.disable.services.location}")
    private File disableServicesLocation;

    private Map<String, ServiceConfig> serviceMap;

    public String getServicesConfig() {
        return this.servicesConfig;
    }

    public File getDisableServicesLocation() {
        return this.disableServicesLocation;
    }

    public ServiceConfig getServiceInfo(String serviceName) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Retrieving service info for '" + serviceName + "'");
        }

        ServiceConfig service = this.getServices().get(serviceName);
        if (service == null) {
            LOG.warn("No service info found for '" + serviceName + "'");
            throw new ServiceNotFoundException(serviceName);
        }

        return service;
    }

    private Map<String, ServiceConfig> getServices() {
        synchronized (serviceMapLock) {
            if (serviceMap == null) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Service map is empty");
                }
                this.readServices();
            }
        }

        return this.serviceMap;

    }

    private void readServices() {
        File servicesFile = this.findServicesFile();

        if (LOG.isInfoEnabled()) {
            LOG.info("Reading services from file '" + servicesFile.getAbsolutePath() + "'");
        }

        this.serviceMap = new HashMap<String, ServiceConfig>();

        try (InputStreamReader reader = new FileReader(servicesFile)) {

            Type type = new TypeToken<List<ServiceConfig>>() {
            }.getType();

            Gson gson = new Gson();
            List<ServiceConfig> serviceList = gson.fromJson(reader, type);
            for (ServiceConfig serviceConfig : serviceList) {
                if (serviceConfig == null) {
                    LOG.warn("Service configuration contains an empty array element which is ignored");
                } else {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Found service '" + serviceConfig.getName() + "'");
                    }

                    this.serviceMap.put(serviceConfig.getName(), serviceConfig);
                }
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("Found " + serviceList.size() + " services");
            }
        } catch (IOException e) {
            LOG.warn("IOException while reading services configuration: " + e.getMessage());
            throw new HealthCheckException(e);
        }

    }

    /**
     * Tries to find the configured services file.
     * <p>
     * The exceptions thrown do not include the full path of the file as the exception message is returned to the caller
     * and we do not want to include complete file paths in the responses. The log file does contain the complete path.
     *
     * @return The services file.
     */
    private File findServicesFile() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Checking services file: " + this.getServicesConfig());
        }

        File servicesFile = new File("./config/" + this.getServicesConfig());

        if (!servicesFile.exists()) {
            LOG.warn("Services file '" + servicesFile.getAbsolutePath() + "' not found");
            throw new HealthCheckException("Services file '" + this.getServicesConfig() + "' not found");
        }

        if (!servicesFile.isFile()) {
            LOG.warn("Services file '" + servicesFile.getAbsolutePath() + "' is not a file");
            throw new HealthCheckException("Services file '" + this.getServicesConfig() + "' is not a file");
        }

        if (!servicesFile.canRead()) {
            LOG.warn("Services file '" + servicesFile.getAbsolutePath() + "' is not readable");
            throw new HealthCheckException("Services file '" + this.getServicesConfig() + "' is not readable");
        }

        return servicesFile;
    }

    public void resetServicesInfo() {
        synchronized (serviceMapLock) {
            this.serviceMap = null;
        }
    }

    public Set<String> getAllServiceNames() {
        return this.getServices().keySet();
    }

}