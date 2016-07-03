# sdl-healthcheck

A simple webapplication that can check the status of SDL CIS micro services.

## Installation

1. Unzip the distribution zip
2. Configure the services to monitor in `./config/services.json` (a default is provided)
3. Configure logging in `./configure/logback.xml` (change the directory where log files are written)

Optional:
* Change the port the web server is listening on by adding an `application.properties` file in `./config` and set the property `server.port` to the required port number.

## Starting

1. Change to directory where the distribution zip was unpacked
2. Start the web application  `./bin/start.sh`

## Stopping

1. Change to directory where the distribution zip was unpacked
2. Stop the web application  `./bin/stop.sh`

## Usage

Once the web application is started it exposes a number of endpoints that can be used to check the status of a micro service or update the configuration of the web application.
The URLs return an HTTP status code and JSON string with details. If there are no problems with the checked micro service the HTTP status code 200 is returned. Otherwise 500 is returned.

* /status/<servicename>: Check the status of the named service (the service needs to be defined in `./config/services.json`)
* /all: returns the status of all configured services
* /reload: reloads the configured services (from `./config/services.json`)

Assuming that the web application is listening on the default port (8091) the URLs would be:
* http://localhost:8091/status/discovery (to check the discovery micro service)
* http://localhost:8091/all
* http://localhost:8091/reload

## Building from source

To build from source you need to have a Java 8 SDK installed.

1. Check out the required version of the code
2. Run `gradlew.bat build'` if you are on windows or `./gradlew` if you are on Linux/Mac

