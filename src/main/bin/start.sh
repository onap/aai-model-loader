#!/bin/sh
#*******************************************************************************
#  ============LICENSE_START==========================================
#  org.onap.aai
#  ===================================================================
#  Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
#  Copyright © 2017-2018 Amdocs
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

BASEDIR="/opt/app/model-loader/"
AJSC_HOME="$BASEDIR"

if [ -z "$CONFIG_HOME" ]; then
	echo "CONFIG_HOME must be set in order to start up process"
	exit 1
fi

CLASSPATH="$AJSC_HOME/lib/*"
CLASSPATH="$CLASSPATH:$AJSC_HOME/extJars/"
CLASSPATH="$CLASSPATH:$AJSC_HOME/etc/"
PROPS="-DAJSC_HOME=$AJSC_HOME"
PROPS="$PROPS -DAJSC_CONF_HOME=$BASEDIR/bundleconfig/"
PROPS="$PROPS -Dlogback.configurationFile=$BASEDIR/bundleconfig/etc/logback.xml"
PROPS="$PROPS -DAJSC_SHARED_CONFIG=$AJSC_CONF_HOME"
PROPS="$PROPS -DAJSC_SERVICE_NAMESPACE=model-loader"
PROPS="$PROPS -DAJSC_SERVICE_VERSION=v1"
PROPS="$PROPS -Dserver.port=9500"
PROPS="$PROPS -DCONFIG_HOME=$CONFIG_HOME"
JVM_MAX_HEAP=${MAX_HEAP:-1024}

echo $CLASSPATH

exec java -Xmx${JVM_MAX_HEAP}m $PROPS -classpath $CLASSPATH com.att.ajsc.runner.Runner context=// port=9500
