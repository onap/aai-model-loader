#!/bin/bash

###
# ============LICENSE_START=======================================================
# MODEL LOADER SERVICE
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================
###

#
# This script will update the config file, with config values supplied
# through environment variables, if set
# 

CONFIG_FILE=`dirname $0`/../webapps/model-loader/WEB-INF/classes/model-loader.properties

# Distribution client configuration
ENVVAR=DISTR_CLIENT_ACTIVE_SERVER_TLS_AUTH
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.ACTIVE_SERVER_TLS_AUTH/s/.*/ml.distribution.ACTIVE_SERVER_TLS_AUTH=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=DISTR_CLIENT_ASDC_ADDRESS 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.ASDC_ADDRESS/s/.*/ml.distribution.ASDC_ADDRESS=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=DISTR_CLIENT_CONSUMER_GROUP
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.CONSUMER_GROUP/s/.*/ml.distribution.CONSUMER_GROUP=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=DISTR_CLIENT_CONSUMER_ID
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.CONSUMER_ID/s/.*/ml.distribution.CONSUMER_ID=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=DISTR_CLIENT_ENVIRONMENT_NAME
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.ENVIRONMENT_NAME/s/.*/ml.distribution.ENVIRONMENT_NAME=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=DISTR_CLIENT_KEYSTORE_PASSWORD
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.KEYSTORE_PASSWORD/s/.*/ml.distribution.KEYSTORE_PASSWORD=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=DISTR_CLIENT_KEYSTORE_FILE 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.KEYSTORE_FILE/s/.*/ml.distribution.KEYSTORE_FILE=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=DISTR_CLIENT_PASSWORD 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.PASSWORD/s/.*/ml.distribution.PASSWORD=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=DISTR_CLIENT_POLLING_INTERVAL 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.POLLING_INTERVAL/s/.*/ml.distribution.POLLING_INTERVAL=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=DISTR_CLIENT_POLLING_TIMEOUT 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.POLLING_TIMEOUT/s/.*/ml.distribution.POLLING_TIMEOUT=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=DISTR_CLIENT_USER 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.USER/s/.*/ml.distribution.USER=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=DISTR_CLIENT_ARTIFACT_TYPES 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.distribution.ARTIFACT_TYPES/s/.*/ml.distribution.ARTIFACT_TYPES=$ENVVALUE/" $CONFIG_FILE;

  
# Model Loader Application Server REST Client Configuration
ENVVAR=APP_SERVER_BASE_URL
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.aai.BASE_URL/s/.*/ml.aai.BASE_URL=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=APP_SERVER_MODEL_URL 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.aai.MODEL_URL/s/.*/ml.aai.MODEL_URL=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=APP_SERVER_NAMED_QUERY_URL 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.aai.NAMED_QUERY_URL/s/.*/ml.aai.NAMED_QUERY_URL=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=APP_SERVER_VNF_IMAGE_URL 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.aai.VNF_IMAGE_URL/s/.*/ml.aai.VNF_IMAGE_URL=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=APP_SERVER_KEYSTORE_FILE 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.aai.KEYSTORE_FILE/s/.*/ml.aai.KEYSTORE_FILE=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=APP_SERVER_KEYSTORE_PASSWORD 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.aai.KEYSTORE_PASSWORD/s/.*/ml.aai.KEYSTORE_PASSWORD=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=APP_SERVER_AUTH_USER 
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.aai.AUTH_USER/s/.*/ml.aai.AUTH_USER=$ENVVALUE/" $CONFIG_FILE;

ENVVAR=APP_SERVER_AUTH_PASSWORD
ENVVALUE=${!ENVVAR}
ENVVALUE=${ENVVALUE//\//\\/}
[ -z ${!ENVVAR+x} ] \
 || sed -i "/ml.aai.AUTH_PASSWORD/s/.*/ml.aai.AUTH_PASSWORD=$ENVVALUE/" $CONFIG_FILE;
