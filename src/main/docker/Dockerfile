FROM alpine:3.8

ARG MICRO_HOME=/opt/app/model-loader
ARG BIN_HOME=$MICRO_HOME/bin
ARG JAR_FILE=model-loader.jar

RUN apk update

# Install and setup java8
RUN apk add openjdk8 bash iputils
## Set up JAVA_HOME for docker command-line
ENV JAVA_HOME usr/lib/jvm/default-jvm
RUN export JAVA_HOME

# Build up the deployment folder structure
RUN mkdir -p $MICRO_HOME
COPY ${JAR_FILE} $MICRO_HOME/
COPY classes/logback.xml $MICRO_HOME/
RUN mkdir -p $BIN_HOME
COPY *.sh $BIN_HOME
RUN chmod 755 $BIN_HOME/*
RUN ln -s /logs $MICRO_HOME/logs

CMD ["/opt/app/model-loader/bin/start.sh"]
