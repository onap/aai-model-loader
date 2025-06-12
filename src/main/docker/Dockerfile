FROM eclipse-temurin:17-jre-alpine
USER 1000

ENV MICRO_HOME=/opt/app/model-loader
WORKDIR $MICRO_HOME

COPY --chown=1000:1000 /maven/model-loader/ .

ENTRYPOINT ["/bin/sh", "/opt/app/model-loader/bin/start.sh"]
