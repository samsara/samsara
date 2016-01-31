FROM samsara/base-image-jdk8:a33-j8u72

MAINTAINER Samsara's team (https://github.com/samsara/qanal)

ADD ./docker/qanal-supervisor.conf /etc/supervisor/conf.d/
ADD ./target/qanal /opt/qanal/qanal
ADD ./docker/config.edn.tmpl /opt/qanal/conf/config.edn.tmpl
ADD ./docker/configure-and-start.sh /

# qanal logs
VOLUME ["/logs"]

CMD /configure-and-start.sh
