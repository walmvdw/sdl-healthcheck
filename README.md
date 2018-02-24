# sdl-healthcheck
[![Build status](https://ci.appveyor.com/api/projects/status/vcersqylnjt970y1/branch/master?svg=true)](https://ci.appveyor.com/project/kunalshetye/sdl-healthcheck/branch/master)

A simple web application that can check the status of SDL CIS micro services.

The health check web application functions as a simple proxy server. It provides http endpoints that can be called (without OAuth authentication), 
these endpoints then call the configured SDL OData micro service, performs authentication and calls the configured URL. This way you can check if 
the micro service works without having to handle the authentication. 

Typical use case is to configure the endpoints provided in a load balancer which uses the web application to check the availability of a micro service.

In addition to the healt check functionality the web application also provides functionality to temporarily 'disable' a service by placing a file in a 
(configurable) directory. This way it is possible to disable the service from the load balancer without actually stopping the service (making it easier to debug
or check issues).

## Installation

1. Unzip the distribution zip
2. Configure the services to monitor in `./config/services.json` (a default is provided)
3. Configure logging in `./configure/logback.xml` (change the directory where log files are written)

Optional:
* Change the port the web server is listening on by adding an `application.properties` file in `./config` and set the property `server.port` to the required port number.

### Securing passwords in the `services.json` file

Just like in SDL Web 8 (Tridion) cd_* configuration files it is possible to encrypt the passwords in the `services.json` file. Check the SDL Web documentation for the procedure (e.g. as described in point 3 [here](http://docs.sdl.com/LiveContent/content/en-US/SDL%20Web-v1/GUID-9419CF43-DA1E-477F-AEF2-4130115C4C9B)).
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
* <http://localhost:8091/status/discovery> (to check the discovery micro service)
* <http://localhost:8091/all>
* <http://localhost:8091/reload>

## Disabling services

If you want to use the functionality to disable services you need to configure a directory where the indicator files can be placed. This can be configured by 
adding a file named `application.properties` to the `config` directory. In the `application.properties` file a property can be set with the name `config.disable.services.location` 
which points to a directory where indicator files can be placed.

To disable a service named 'discovery' a file (can be empty) must be created in the configured directory with the same name as the service, in this example `discovery`.

## Building from source

To build from source you need to have a Java 8 SDK installed.

1. Check out the required version of the code
2. Run `gradlew.bat build'` if you are on windows or `./gradlew` if you are on Linux/Mac

