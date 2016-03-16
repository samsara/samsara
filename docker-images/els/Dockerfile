FROM samsara/base-image-jdk8:a33-j8u72

MAINTAINER Samsara's team (https://github.com/samsara/samsara/docker-images)

#
# Kafka installation
#
ENV ELS_VERSION 2.2.1


RUN wget --progress=dot:mega --no-check-certificate --no-cookies \
    "https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/tar/elasticsearch/${ELS_VERSION}/elasticsearch-${ELS_VERSION}.tar.gz" -O - | tar -xzf - -C /opt && \
    ln -s /opt/elasticsearch-* /opt/els && \
    mv /opt/els/config/elasticsearch.yml /opt/els/config/elasticsearch.yml.orig


VOLUME ["/data", "/logs"]

## maybe need to install cloud plugins as well
RUN /opt/els/bin/plugin install royrusso/elasticsearch-HQ/v2.0.3 && \
    /opt/els/bin/plugin install lmenezes/elasticsearch-kopf/v2.1.1
#    /opt/els/bin/plugin install elasticsearch/elasticsearch-cloud-aws/2.7.1


ADD ./elasticsearch.yml.tmpl  /opt/els/config/elasticsearch.yml.tmpl
ADD ./els-supervisor.conf.tmpl /etc/supervisor/conf.d/els-supervisor.conf.tmpl
ADD ./configure-and-start.sh /configure-and-start.sh

# expose client port, peer port and supervisord port
EXPOSE 9200 9300 15000

CMD /configure-and-start.sh
