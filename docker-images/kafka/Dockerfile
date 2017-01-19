FROM samsara/base-image-jdk8:a33-j8u72

MAINTAINER Samsara's team (https://github.com/samsara/samsara/docker-images)

#
# Kafka installation
#
ENV KAFKA_VERSION 0.10.1.0
ENV SCALA_VERSION 2.11

# http://apache.mirrors.pair.com/kafka
RUN curl -sSL "http://mirrors.ukfast.co.uk/sites/ftp.apache.org/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz" | tar -zxvf - -C /opt && \
    ln -fs /opt/kafka_* /opt/kafka && \
    mv /opt/kafka/config/server.properties /opt/kafka/config/server.properties.orig && \
    wget --progress=dot:kilo --no-check-certificate \
    "https://bintray.com/artifact/download/airbnb/jars/com/airbnb/kafka-statsd-metrics2/0.4.1/kafka-statsd-metrics2-0.4.1.jar" \
    -O /opt/kafka/libs/kafka-statsd-metrics2.jar && \
    wget --progress=dot:kilo --no-check-certificate \
    http://search.maven.org/remotecontent?filepath=com/indeed/java-dogstatsd-client/2.0.11/java-dogstatsd-client-2.0.11.jar \
    -O /opt/kafka/libs/java-dogstatsd-client-2.0.11.jar

ENV KAFKA_HOME /opt/kafka
VOLUME ["/data", "/logs"]
# DEFAULT KAFKA_BROKER_PORT is 9092
EXPOSE 15000

ADD ./server.properties.tmpl  /opt/kafka/config/server.properties.tmpl
ADD ./kafka-supervisor.conf.tmpl /etc/supervisor/conf.d/kafka-supervisor.conf.tmpl
ADD ./configure-and-start.sh /configure-and-start.sh

CMD /configure-and-start.sh
