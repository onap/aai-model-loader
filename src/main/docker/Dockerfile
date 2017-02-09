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

FROM ubuntu:14.04
ARG jettyrel=9.3.9.v20160517
ARG jettydist=jetty-distribution-${jettyrel}
ARG jettybase=/opt/jetty

RUN apt-get update && apt-get --force-yes -y -f install wget

# Install java8
RUN apt-get install -y software-properties-common

# sudo -E is required to preserve the environment
# If you remove that line, it will most like freeze at this step

RUN sudo -E add-apt-repository ppa:openjdk-r/ppa && apt-get update && apt-get install -y openjdk-8-jdk

# Setup JAVA_HOME, this is useful for docker commandline

ENV JAVA_HOME usr/lib/jvm/java-8-openjdk-amd64
RUN export JAVA_HOME

RUN wget http://central.maven.org/maven2/org/eclipse/jetty/jetty-distribution/${jettyrel}/${jettydist}.tar.gz
RUN gunzip ${jettydist}.tar.gz && tar xvf ${jettydist}.tar

COPY model-loader* $jettydist/webapps/model-loader/

COPY startup.sh update_config.sh ${jettydist}/bin/
RUN chmod 700 ${jettydist}/bin/startup.sh && chmod 700 ${jettydist}/bin/update_config.sh
RUN mkdir -p ${jettybase}
RUN mv ${jettydist} ${jettybase}/${jettydist}
RUN rm -rf $jettybase/$jettydist/demo-base

CMD /opt/jetty/*/bin/startup.sh
