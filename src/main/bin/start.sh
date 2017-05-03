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

#!/bin/sh

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
PROPS="$PROPS -Dserver.port=8080"
PROPS="$PROPS -DCONFIG_HOME=$CONFIG_HOME"

echo $CLASSPATH

java -Xms1024m -Xmx4096m -XX:PermSize=2024m $PROPS -classpath $CLASSPATH com.att.ajsc.runner.Runner context=// sslport=8081
