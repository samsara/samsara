FROM samsara/base-image-jdk8:u1410-j8u40

MAINTAINER Samsara's team (https://github.com/samsara/samsara/docker-images)

#
# Spark installation
#
ENV SPARK_VERSION 1.4.1

RUN \
    curl -sSL "http://apache.mirrors.pair.com/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop2.6.tgz" | tar -zxvf - -C /opt && \
    ln -fs /opt/spark-* /opt/spark


ENV SPARK_HOME /opt/spark
VOLUME ["/data", "/logs"]

EXPOSE 7077 7078 8080 8081 15000

ADD ./spark-env.sh.tmpl  /opt/spark/conf/spark-env.sh.tmpl
ADD ./spark-worker-supervisor.conf /etc/supervisor/conf.d/spark-worker-supervisor.conf
ADD ./spark-starter.sh /opt/spark/spark-starter.sh
ADD ./configure-and-start.sh /configure-and-start.sh

CMD /configure-and-start.sh
