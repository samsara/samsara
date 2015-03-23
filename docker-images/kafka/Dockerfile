FROM samsara/base-image-jdk7:u1410-j7u75

MAINTAINER Samsara's team (https://github.com/samsara/samsara-docker-images)

#
# Kafka installation
#
ENV KAFKA_VERSION 0.8.2.1
ENV SCALA_VERSION 2.10

RUN curl -sSL "http://apache.mirrors.pair.com/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz" | tar -zxvf - -C /opt && \
    ln -fs /opt/kafka_* /opt/kafka && \
    mv /opt/kafka/config/server.properties /opt/kafka/config/server.properties.orig

ENV KAFKA_HOME /opt/kafka
VOLUME ["/data", "/logs"]

ADD ./server.properties.tmpl  /opt/kafka/config/server.properties.tmpl
ADD ./kafka-statsd-metrics2-0.4.0-SNP.jar /opt/kafka/libs/kafka-statsd-metrics2-0.4.0-SNP.jar
ADD ./kafka-supervisor.conf /etc/supervisor/conf.d/kafka-supervisor.conf
ADD ./configure-and-start.sh /configure-and-start.sh

CMD /configure-and-start.sh
