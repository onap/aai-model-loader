FROM openjdk:8-alpine3.9

ARG MICRO_HOME=/opt/app/model-loader
ARG BIN_HOME=$MICRO_HOME/bin
ARG JAR_FILE=model-loader.jar

# Build up the deployment folder structure
RUN mkdir -p $MICRO_HOME \
    && mkdir -p $BIN_HOME
COPY ${JAR_FILE} classes/logback.xml $MICRO_HOME/
COPY *.sh $BIN_HOME
RUN chmod 755 $BIN_HOME/* \
    && ln -s /logs $MICRO_HOME/logs

CMD ["/opt/app/model-loader/bin/start.sh"]
