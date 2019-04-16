FROM aaionap/aai-common:@aai.docker.version@-@aai.base.image@

ARG MICRO_HOME=/opt/app/model-loader

# Build up the deployment folder structure
RUN mkdir -p $MICRO_HOME
WORKDIR $MICRO_HOME
COPY /maven/model-loader/ .
RUN chmod 755 bin/* \
    && ln -s /logs $MICRO_HOME/logs

CMD ["/opt/app/model-loader/bin/start.sh"]
