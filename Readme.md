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
A Model Loader docker image can be created by running `docker build -t openecomp/model-loader target`

## Running Model Loader 

### Deploying The Micro Service 

Push the Docker image that you have built to your Docker repository and pull it down to the location that you will be running the search service from.

**Create the following directories on the host machine:**

    ./logs
    ./opt/app/model-loader/appconfig
    ./opt/app/model-loader/appconfig/auth
    
You will be mounting these as data volumes when you start the Docker container.  For examples of the files required in these directories, see the aai/test/config repository (https://gerrit.onap.org/r/#/admin/projects/aai/test-config)

**Populate these directories as follows:**

##### Contents of /opt/app/model-loader/appconfig

The following file must be present in this directory on the host machine:
    
_model-loader.properties_  
    # Always false.  TLS Auth currently not supported 
    ml.distribution.ACTIVE_SERVER_TLS_AUTH=false
    
    # Address/port of the SDC
    ml.distribution.ASDC_ADDRESS=<SDC-Hostname>:8443
    
    # DMaaP consumer group.  
    ml.distribution.CONSUMER_GROUP=aai-ml-group
    
    # DMaaP consumer ID
    ml.distribution.CONSUMER_ID=aai-ml
    
    # SDC Environment Name.  This must match the environment name configured on the SDC
    ml.distribution.ENVIRONMENT_NAME=<Environment Name>
    
    # Currently not used
    ml.distribution.KEYSTORE_PASSWORD=
    
    # Currently not used
    ml.distribution.KEYSTORE_FILE=
    
    # Obfuscated password to connect to the SDC.  To obtain this value, use the following Jetty library to 
    # obfuscate the cleartext password:  http://www.eclipse.org/jetty/documentation/9.4.x/configuring-security-secure-passwords.html
    ml.distribution.PASSWORD=OBF:<password>
    
    # How often (in seconds) to poll the DMaaP cluster for new model events
    ml.distribution.POLLING_INTERVAL=<integer>
    
    # Timeout value (in seconds) when polling DMaaP for new model events
    ml.distribution.POLLING_TIMEOUT=<integer>
    
    # Username to use when connecting to the SDC
    ml.distribution.USER=<username>
    
    # Artifact type we want to download from the SDC (the values below will typically suffice)
    ml.distribution.ARTIFACT_TYPES=MODEL_INVENTORY_PROFILE,MODEL_QUERY_SPEC,VNF_CATALOG

    # URL of the A&AI
    ml.aai.BASE_URL=https://<AAI-Hostname>:8443
    
    # A&AI endpoint to post models
    ml.aai.MODEL_URL=/aai/v*/service-design-and-creation/models/model/
    
    # A&AI endpoint to post named queries
    ml.aai.NAMED_QUERY_URL=/aai/v*/service-design-and-creation/named-queries/named-query/
    
    # A&AI endpoint to post vnf images
    ml.aai.VNF_IMAGE_URL=/aai/v8/service-design-and-creation/vnf-images
    
    # Name of certificate to use in connecting to the A&AI
    ml.aai.KEYSTORE_FILE=aai-os-cert.p12
    
    # Obfuscated keystore password to connect to the A&AI.  This is only required if using 2-way SSL (not basic auth).
    # To obtain this value, use the following Jetty library to obfuscate the cleartext password:
    # http://www.eclipse.org/jetty/documentation/9.4.x/configuring-security-secure-passwords.html
    ml.aai.KEYSTORE_PASSWORD=OBF:<password>
    
    # Name of user to use when connecting to the A&AI.  This is only required if using basic auth (not 2-way SSL).
    ml.aai.AUTH_USER=<username>
    
    # Obfuscated password to connect to the A&AI.  This is only required if using basic auth (not 2-way SSL).
    # To obtain this value, use the following Jetty library to obfuscate the cleartext password:
    # http://www.eclipse.org/jetty/documentation/9.4.x/configuring-security-secure-passwords.html
    ml.aai.AUTH_PASSWORD=OBF:<password>
    


##### Contents of the /opt/app/model-loader/app-config/auth Directory

The following files must be present in this directory on the host machine:

_aai-os-cert.p12_

The certificate used to connected to the A&AI

**Start the service:**

You can now start the Docker container for the _Search Data Service_, in the following manner:

	docker run -d \
		-e CONFIG_HOME=/opt/app/model-loader/config/ \
	    -v /logs:/logs \
	    -v /opt/app/model-loader/appconfig:/opt/app/model-loader/config \
	    --name model-loader \
	    {{your docker repo}}/model-loader
    
Where,

    {{your docker repo}} = The Docker repository you have published your image to.
