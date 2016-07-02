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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * HealthCheckApplication
 */
@SpringBootApplication
public class HealthCheckApplication {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckApplication.class);

    public static void main(String[] args) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting ServiceHealthCheck Application");
        }

        SpringApplication app = new SpringApplication(HealthCheckApplication.class);
        app.setBannerMode(Banner.Mode.OFF);

        //ApplicationContext ctx = app.run(args);
        app.run(args);

        System.out.println("Health Check Monitor started");
    }

}
