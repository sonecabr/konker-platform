FROM adoptopenjdk/openjdk8:jre8u232-b09-alpine

LABEL maintainer="Douglas Apolinário <douglas@konkerlabs.com>"

ENV KONKER_BASE /var/lib/konker
RUN mkdir -p "$KONKER_BASE"

WORKDIR $KONKER_BASE

COPY docker-resources/docker-entrypoint.sh /
RUN chmod 777 /docker-entrypoint.sh

COPY target/cassandra-etl.jar /var/lib/konker/
COPY docker-resources/dist/application.conf /var/lib/konker/
COPY docker-resources/dist/logback.xml /var/lib/konker/

#start
EXPOSE 80
ENTRYPOINT ["/docker-entrypoint.sh"]