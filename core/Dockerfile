FROM samsara/base-image-jdk8:u1410-j8u40

MAINTAINER Samsara's team (https://github.com/samsara/samsara-core)

ADD ./target/samsara-core           /opt/samsara-core/
ADD ./config/config.edn.tmpl        /opt/samsara-core/config/config.edn.tmpl
ADD ./docker/configure-and-start.sh /configure-and-start.sh

ADD ./docker/samsara-core-supervisor.conf /etc/supervisor/conf.d/

# Available ports
# 15000 - supervisor admin interface
# 4500  - nREPL
EXPOSE 15000 4555

# ingestion-api logs
VOLUME ["/logs"]

CMD /configure-and-start.sh
