FROM samsara/base-image-jdk8:a33-j8u72

MAINTAINER Samsara's team (https://github.com/samsara/samsara/docker-images)

#
# Zookeeper installation
#
ENV ZK_VERSION 3.5.1-alpha

# http://apache.mirrors.pair.com/zookeeper
RUN curl -sSL http://mirrors.ukfast.co.uk/sites/ftp.apache.org/zookeeper/zookeeper-${ZK_VERSION}/zookeeper-${ZK_VERSION}.tar.gz | tar -xzf - -C /opt && \
    ln -fs /opt/zookeeper-* /opt/zookeeper


EXPOSE 2181 2888 3888 15000

VOLUME ["/data", "/logs"]

ADD ./zookeeper-supervisor.conf /etc/supervisor/conf.d/zookeeper-supervisor.conf
ADD ./configure-and-start.sh /configure-and-start.sh

CMD /configure-and-start.sh
