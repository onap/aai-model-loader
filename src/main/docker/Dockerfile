FROM @aai.docker.namespace@/aai-common-@aai.base.image@:@aai.base.image.version@

ARG MICRO_HOME=/opt/app/model-loader

WORKDIR $MICRO_HOME

USER root

RUN mkdir -p /opt/aaihome $MICRO_HOME /logs && \
    ln -s /logs $MICRO_HOME/logs && \
    chown -R aaiadmin:aaiadmin $MICRO_HOME && \
    chown -R aaiadmin:aaiadmin /logs

COPY --chown=aaiadmin:aaiadmin /maven/model-loader/ .

ENTRYPOINT ["/bin/bash", "/opt/app/model-loader/bin/start.sh"]

USER aaiadmin
