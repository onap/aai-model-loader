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

exec java -Xms1024m -Xmx4096m -XX:PermSize=2024m $PROPS -classpath $CLASSPATH com.att.ajsc.runner.Runner context=// sslport=8081
