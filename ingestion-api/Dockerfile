FROM relateiq/oracle-java8

ADD ./target/ingestion-api /opt/ingestion-api/
ADD ./config               /opt/ingestion-api/config

RUN apt-get install -y supervisor
ADD ./docker/supervisord.conf /etc/supervisor/supervisord.conf
ADD ./docker/ingestion-api-supervisor.conf /etc/supervisor/conf.d/
ADD ./docker/start.sh /

# Available ports
# 9000  - http ingestion api
# 15000 - supervisor admin interface
EXPOSE 9000 15000

# ingestion-api logs
VOLUME ["/logs"]

CMD /start.sh
