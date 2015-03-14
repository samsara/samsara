FROM relateiq/oracle-java8

ADD ./target/ingestion-api /opt/ingestion-api/
ADD ./config               /opt/ingestion-api/config

RUN apt-get install -y supervisor
ADD ./docker/supervisord.conf /etc/supervisor/supervisord.conf
ADD ./docker/ingestion-api-supervisor.conf /etc/supervisor/conf.d/

EXPOSE 9000 15001

VOLUME ["/logs"]

CMD /bin/sh -c "/usr/bin/supervisord -c /etc/supervisor/supervisord.conf ; while true ; do /usr/bin/supervisorctl ; done"
