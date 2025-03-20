FROM eclipse-temurin:11-jre-alpine
USER nobody

ENV MICRO_HOME=/opt/app/model-loader
WORKDIR $MICRO_HOME

COPY --chown=nobody:nobody /maven/model-loader/ .

ENTRYPOINT ["/bin/sh", "/opt/app/model-loader/bin/start.sh"]
