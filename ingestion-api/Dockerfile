FROM samsara/base-image-jdk8:u1410-j8u40

MAINTAINER Samsara's team (https://github.com/samsara/samsara-ingestion-api)

ADD ./target/ingestion-api          /opt/ingestion-api/
ADD ./config/config.edn.tmpl        /opt/ingestion-api/config/config.edn.tmpl
ADD ./docker/configure-and-start.sh /configure-and-start.sh

ADD ./docker/ingestion-api-supervisor.conf /etc/supervisor/conf.d/

# Available ports
# 9000  - http ingestion api
# 15000 - supervisor admin interface
EXPOSE 9000 15000

# ingestion-api logs
VOLUME ["/logs"]

CMD /configure-and-start.sh
