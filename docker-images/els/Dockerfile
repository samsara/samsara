FROM samsara/base-image-jdk8:a33-j8u72

MAINTAINER Samsara's team (https://github.com/samsara/samsara/docker-images)

#
# Kafka installation
#
ENV ELS_VERSION 1.6.2


RUN wget --progress=dot:mega --no-check-certificate --no-cookies \
    "https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-${ELS_VERSION}.tar.gz" -O - | tar -xzf - -C /opt && \
    ln -s /opt/elasticsearch-* /opt/els && \
    mv /opt/els/config/elasticsearch.yml /opt/els/config/elasticsearch.yml.orig


VOLUME ["/data", "/logs"]

ADD ./elasticsearch.yml.min  /opt/els/config/elasticsearch.yml

## maybe need to install cloud plugins as well
RUN /opt/els/bin/plugin -install royrusso/elasticsearch-HQ && \
    /opt/els/bin/plugin -install mobz/elasticsearch-head && \
    /opt/els/bin/plugin -install lmenezes/elasticsearch-kopf && \
    /opt/els/bin/plugin -install elasticsearch/elasticsearch-cloud-aws/2.6.1 && \
    /opt/els/bin/plugin -url https://github.com/grmblfrz/elasticsearch-zookeeper/releases/download/v1.6.1/elasticsearch-zookeeper-1.6.1.zip -install zookeeper

ADD ./elasticsearch.yml.tmpl  /opt/els/config/elasticsearch.yml.tmpl
ADD ./els-supervisor.conf.tmpl /etc/supervisor/conf.d/els-supervisor.conf.tmpl
ADD ./configure-and-start.sh /configure-and-start.sh

# expose client port, peer port and supervisord port
EXPOSE 9200 9300 15000

CMD /configure-and-start.sh
