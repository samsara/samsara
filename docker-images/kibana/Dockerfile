FROM samsara/base-image-jdk8:a33-j8u72

MAINTAINER Samsara's team (https://github.com/samsara/samsara/docker-images)

#
# Kibana installation
#

RUN export KIBANA_VERSION=4.4.2 && \
    curl -sSL "https://download.elastic.co/kibana/kibana/kibana-${KIBANA_VERSION}-linux-x64.tar.gz" | tar -zxvf - -C /opt && \
    ln -s /opt/kibana-* /opt/kibana && \
    /opt/kibana/bin/kibana plugin --install elasticsearch/marvel/latest && \
    /opt/kibana/bin/kibana plugin --install elastic/sense && \
    mv /opt/kibana/config/kibana.yml /opt/kibana/config/kibana.yml.orig

### VOLUME ["/data", "/logs"]

ADD ./kibana.yml.tmpl  /opt/kibana/config/kibana.yml.tmpl
ADD ./kibana-supervisor.conf /etc/supervisor/conf.d/kibana-supervisor.conf
ADD ./configure-and-start.sh /configure-and-start.sh

### # expose client port, peer port and supervisord port
EXPOSE 8000 15000

CMD /configure-and-start.sh
