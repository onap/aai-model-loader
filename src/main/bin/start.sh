#!/bin/sh
#*******************************************************************************
#  ============LICENSE_START==========================================
#  org.onap.aai
#  ===================================================================
#  Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
#  Copyright © 2017-2018 European Software Marketing Ltd.
#  ===================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END============================================
#*******************************************************************************

# AJSC_HOME is required for EELF logging.
# This path is referenced in the file logback.xml.
AJSC_HOME="${AJSC_HOME-/opt/app/model-loader}"

if [ -z "$CONFIG_HOME" ]; then
    echo "CONFIG_HOME must be set in order to start up the process"
    echo "The expected value is a folder containing the model-loader.properties file"
    exit 1
fi

JARFILE="$AJSC_HOME/model-loader.jar"

# Some properties are repeated here for debugging purposes.
PROPS="-DAJSC_HOME=$AJSC_HOME"
PROPS="$PROPS -DCONFIG_HOME=$CONFIG_HOME"
PROPS="$PROPS -Dcom.att.eelf.logging.path=$AJSC_HOME"
PROPS="$PROPS -Dcom.att.eelf.logging.file=logback.xml"
PROPS="$PROPS -Dlogback.configurationFile=$AJSC_HOME/logback.xml"
PROPS="$PROPS -Dserver.port=9500"
JVM_MAX_HEAP=${MAX_HEAP:-1024}

if [ -z "${java_runtime_arguments}" ]; then
  java_runtime_arguments="-Xms75m -Xmx${JVM_MAX_HEAP}m \
 -Dcom.sun.management.jmxremote \
 -Dcom.sun.management.jmxremote.authenticate=false \
 -Dcom.sun.management.jmxremote.ssl=false \
 -Dcom.sun.management.jmxremote.local.only=false \
 -Dcom.sun.management.jmxremote.port=1099 \
 -Dcom.sun.management.jmxremote.rmi.port=1099 \
 -Djava.rmi.server.hostname=127.0.0.1"
fi

echo "java $java_runtime_arguments $PROPS -jar $JARFILE"
if [ -z "$RUN_MS_AS_ROOT" ] ; then
java $java_runtime_arguments $PROPS -jar $JARFILE
else 
sudo -E java $java_runtime_arguments $PROPS -jar $JARFILE
fi
