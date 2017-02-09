# Introduction

The A&AI Model Loader Service is an application that facilitates
distribution, ingestion of new service and resource models, and vnf
catalogs from the SDC to the A&AI.

## Features

The Model Loader:

* registers with the SDC to receive notification events 
* polls the UEB/DMaap cluster for notification events
* downloads artifacts from SDC upon receipt of a distribution event
* pushes distribution components to A&AI
		    
## Compiling Model Loader

Model Loader can be compiled by running `mvn clean install`

## Running Model Loader 

### Create a config file with the following key/values:

```
DISTR_CLIENT_ASDC_ADDRESS=<SDC_ADDRESS>
DISTR_CLIENT_CONSUMER_GROUP=<UEB_CONSUMER_GROUP>  ;;  Uniquely identiy this group of model loaders.
DISTR_CLIENT_CONSUMER_ID=<UEB_CONSUMER_GROUP_ID>  ;;  Uniquely identiythis model loader.
DISTR_CLIENT_ENVIRONMENT_NAME=<ENVIRONMENT_NAME>  ;;  Environment name configured on the SDC
DISTR_CLIENT_PASSWORD=<DISTR_PASSWORD>            ;;  Password to connect to SDC
DISTR_CLIENT_USER=<USER_ID>                       ;;  User name to connect to SDC
		     
APP_SERVER_BASE_URL=https://<aai-address>:8443    ;; AAI Address (URL)
APP_SERVER_AUTH_USER=<USER_ID>                    ;; User name to connect to AAI
APP_SERVER_AUTH_PASSWORD=<PASSWORD>               ;; Password to connect to AAi

```

### Docker 

#### Build your own Model Loader docker image and create docker containers
1. mvn clean package docker:build                 ;; Build a docker image of Model Loader
2. sudo docker images                             ;; list docker images and check if the image you build is listed.
3. sudo docker run --env-file <config-filename> <model-loader-image> /opt/jetty/jetty*/bin/startup.sh


#### Retrieve logs from stopped container
* docker cp <container-id>:/opt/jetty/jetty-distribution-9.3.9.v20160517/logs/AAI-ML/error.log /tmp/
