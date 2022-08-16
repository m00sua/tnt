# FedEx Assessment

## Pre-requisites
* OS: Win / Linux
* Java: v8 
* Maven: v3.5 
* Docker: `xyzassessment/backend-services` is up and running on 8080 port

## Build

Open the command line in the projects folder and run the `build.bat` or `mvn clean verify` command.

## Run

Be sure that build was done successfully.
Open the command line in the projects folder and run the `run.bat` or `java -jar ./target/tnt-1.0-SNAPSHOT.jar` command.

Server port is 8090. 
* If this port is not ok, you can create local config file `application.properties` and add there `server.port=<needed port>` config, then restart the service.

## REST API

This service opens this endpoint:

* method: `GET`
* URL: `http://localhost:8090/aggregation` with non-required parameters `pricing`, `track` and `shipments`
  * for example: `http://localhost:8090/aggregation?pricing=NL,CN&track=109347263,123456891&shipments=109347263,123456891`
* More info in Back-End_Java_Assessment_FedEx.pdf


Good luck!
