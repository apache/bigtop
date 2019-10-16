ARG PRESTO_IMAGE=prestosql/presto:latest
FROM $PRESTO_IMAGE

LABEL maintainer="Apache Bogtop community <dev@bigtop.apache.org>"
LABEL url="https://bigtop.apache.org"

ENV PRESTO_HOME=/usr/lib/presto

COPY minio.properties $PRESTO_HOME/etc/catalog/minio.properties
COPY config.properties $PRESTO_HOME/etc/config.properties
COPY jvm.config $PRESTO_HOME/etc/jvm.config
COPY node.properties $PRESTO_HOME/etc/node.properties
