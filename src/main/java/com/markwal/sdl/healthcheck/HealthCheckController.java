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

import com.markwal.sdl.healthcheck.config.Configuration;
import com.markwal.sdl.healthcheck.config.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
public class HealthCheckController {

    private final String dashboardUrl = "/dashboard.html";
    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckController.class);
    private final Object connectionsLock = new Object();
    @Autowired
    private Configuration config;
    private Map<String, ServiceConnection> connections = new HashMap<String, ServiceConnection>();

    @RequestMapping(value = "/")
    public void defaultMethod(HttpServletResponse response) throws IOException {
        response.sendRedirect(dashboardUrl);
    }

    @RequestMapping(value = "/reload")
    public String reload() {
        this.config.resetServicesInfo();
        this.clearConnections();
        return "ok";
    }

    private void clearConnections() {
        synchronized (this.connectionsLock) {
            // there are no resources in the connection that need closing, so we just empty the map
            this.connections.clear();
        }
    }

    @RequestMapping(value = "/all", produces = "application/json")
    public
    @ResponseBody
    List<ServiceStatus> all() {
        List<ServiceStatus> statuses = new ArrayList<ServiceStatus>();

        for (String name : this.config.getAllServiceNames()) {
            ServiceStatus status;

            try {
                status = this.checkService(name);
            } catch (HealthCheckException e) {
                LOG.warn("Exception while checking status for service '" + name + " ': " + e.getMessage(), e);
                status = new ServiceStatus(name, "error", "Exception: " + e.getMessage());
            }
            statuses.add(status);
        }

        return statuses;
    }

    @RequestMapping(value = "/status/{serviceName}", produces = "application/json")
    public
    @ResponseBody
    ResponseEntity<ServiceStatus> status(@PathVariable String serviceName) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Request /status for service " + serviceName);
        }

        ServiceStatus status = this.checkService(serviceName);

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

    private ServiceStatus checkService(String serviceName) {

        if (this.isServiceDisabled(serviceName)) {
            return new ServiceStatus(serviceName, "disabled", "Disabled by status file");
        }

        ServiceConnection checker = this.getServiceConnection(serviceName);
        ServiceStatus serviceStatus = checker.checkStatus();

        return serviceStatus;
    }

    /**
     * Checks if a service is disabled. A service is disabled if a file with the same name as the service name exists
     * in the 'disable services location' which is set by a property 'config.disable.services.location' in the
     * application.properties file.
     * <p>
     * Note that no check is done if the service actually exists in the configuration, if a file with the service name
     * exists a status of 'disabled' is returned (for performance reasons).
     *
     * @param serviceName The name of the service to check.
     * @return True if the service is disabled.
     */
    private boolean isServiceDisabled(String serviceName) {
        File disableServicesLocation = config.getDisableServicesLocation();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Checking if service '" + serviceName + "' is disabled; check location: '" + disableServicesLocation + "'");
        }

        if (disableServicesLocation != null && disableServicesLocation.isDirectory()) {
            File disabledFile = new File(config.getDisableServicesLocation(), serviceName);
            if (disabledFile.exists()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Service '" + serviceName + "' is disabled by file: '" + disabledFile.getAbsolutePath() + "'");
                }
                return true;
            } else if (LOG.isTraceEnabled()) {
                LOG.info("File: '" + disabledFile.getAbsolutePath() + "' does not exist, service is not disabbled.");

            }
        }

        return false;
    }

    private ServiceConnection getServiceConnection(String serviceName) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Looking up connection to service: " + serviceName);
        }

        synchronized (this.connectionsLock) {
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

    }

    @ExceptionHandler(ServiceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public
    @ResponseBody
    ErrorResponse handleServiceNotFoundException(Exception exc) {
        LOG.warn("ServiceNotFoundException", exc);
        return new ErrorResponse("ServiceNotFoundException: " + exc.getMessage());
    }

    @ExceptionHandler(TokenException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public
    @ResponseBody
    ErrorResponse handleTokenException(Exception exc) {
        LOG.warn("TokenException", exc);
        return new ErrorResponse("TokenException: " + exc.getMessage());
    }

    @ExceptionHandler(HealthCheckException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public
    @ResponseBody
    ErrorResponse handleHealthCheckException(Exception exc) {
        LOG.warn("HealthCheckException", exc);
        return new ErrorResponse("HealthCheckException: " + exc.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public
    @ResponseBody
    ErrorResponse handleException(Exception exc) {
        LOG.warn("Exception", exc);
        return new ErrorResponse("Exception: " + exc.getClass().getName() + ": " + exc.getMessage());
    }


}