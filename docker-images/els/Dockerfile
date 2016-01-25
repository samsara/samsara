FROM samsara/base-image-jdk8:u1410-j8u40

MAINTAINER Samsara's team (https://github.com/samsara/samsara/docker-images)

#
# Kafka installation
#
ENV ELS_VERSION 1.6.2


RUN wget -nv "https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-${ELS_VERSION}.deb" -O /tmp/els.deb && \
    dpkg -i /tmp/els.deb && \
    rm -fr /tmp/els.deb && \
    mv /etc/elasticsearch/elasticsearch.yml /etc/elasticsearch/elasticsearch.yml.orig


VOLUME ["/data", "/logs"]

ADD ./elasticsearch.yml.min  /etc/elasticsearch/elasticsearch.yml

## maybe need to install cloud plugins as well
RUN /usr/share/elasticsearch/bin/plugin -install royrusso/elasticsearch-HQ && \
    /usr/share/elasticsearch/bin/plugin -install mobz/elasticsearch-head && \
    /usr/share/elasticsearch/bin/plugin -install lmenezes/elasticsearch-kopf && \
    /usr/share/elasticsearch/bin/plugin -install elasticsearch/elasticsearch-cloud-aws/2.6.1 && \
    /usr/share/elasticsearch/bin/plugin -url https://github.com/grmblfrz/elasticsearch-zookeeper/releases/download/v1.6.1/elasticsearch-zookeeper-1.6.1.zip -install zookeeper

ADD ./elasticsearch.yml.tmpl  /etc/elasticsearch/elasticsearch.yml.tmpl
ADD ./els-supervisor.conf.tmpl /etc/supervisor/conf.d/els-supervisor.conf.tmpl
ADD ./configure-and-start.sh /configure-and-start.sh

# expose client port, peer port and supervisord port
EXPOSE 9200 9300 15000

CMD /configure-and-start.sh
