FROM samsara/base-image:latest

MAINTAINER Samsara's team (https://github.com/samsara/samsara-docker-images)

#
# Kafka installation
#
ENV KAFKA_VERSION 0.8.2.1
ENV SCALA_VERSION 2.10

ADD ./download-kafka.sh /tmp/download-kafka.sh
RUN /tmp/download-kafka.sh
RUN cp /opt/kafka/config/server.properties /opt/kafka/config/server.properties.orig && \
    rm -f /tmp/download-kafka.sh

ENV KAFKA_HOME /opt/kafka
VOLUME ["/data", "/logs"]

ADD ./server.properties.tmpl  /opt/kafka/config/server.properties.tmpl
ADD ./kafka-statsd-metrics2-0.4.0-SNP.jar /opt/kafka/libs/kafka-statsd-metrics2-0.4.0-SNP.jar
ADD ./kafka-supervisor.conf /etc/supervisor/conf.d/kafka-supervisor.conf
ADD ./configure-and-start.sh /configure-and-start.sh

CMD /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
