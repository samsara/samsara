FROM samsara/base-image-jdk8:u1410-j8u40

MAINTAINER Samsara's team (https://github.com/samsara/samsara-docker-images)

#
# Riemann installation
#
RUN export RIEMANN_VERSION=0.2.9 && \
    wget --progress=dot:mega --no-check-certificate \
    https://aphyr.com/riemann/riemann-${RIEMANN_VERSION}.tar.bz2 -O - \
    | tar -jxvf - -C /opt && \
    ln -s /opt/riemann-* /opt/riemann && \
    mv /opt/riemann/etc/riemann.config /opt/riemann/etc/riemann.config.orig

VOLUME ["/data", "/logs"]

ADD ./riemann.config  /opt/riemann/etc/riemann.config
ADD ./riemann-supervisor.conf /etc/supervisor/conf.d/riemann-supervisor.conf


#
# InfluxDB installation
#
RUN export INFLUXDB_VERSION=0.8.8 && \
    wget --progress=dot:mega --no-check-certificate \
    https://s3.amazonaws.com/influxdb/influxdb_${INFLUXDB_VERSION}_amd64.deb -O /tmp/influxdb.deb && \
    dpkg -i /tmp/influxdb.deb && \
    rm /tmp/influxdb.deb

ADD ./config.toml /opt/influxdb/config/config.toml
ADD ./influxdb-supervisor.conf /etc/supervisor/conf.d/influxdb-supervisor.conf


#
# Nginx/Grafana installation
#
RUN export GRAFANA_VERSION=1.9.1 && \
    apt-get update && \
    apt-get install -y nginx pwgen apache2-utils && \
    wget --progress=dot:mega --no-check-certificate \
    http://grafanarel.s3.amazonaws.com/grafana-${GRAFANA_VERSION}.tar.gz -O - \
    | tar -zxvf - -C /opt/ && \
    ln -s /opt/grafana-* /opt/grafana && \
    echo "daemon off;" >> /etc/nginx/nginx.conf

ADD ./default /etc/nginx/sites-enabled/default
ADD ./config.js /opt/grafana/config.js
ADD ./grafana-supervisor.conf /etc/supervisor/conf.d/grafana-supervisor.conf

ADD ./configure-and-start.sh /configure-and-start.sh

# expose grafana, rieman ports,      influx,   supervisord
EXPOSE 80         5555 5555/udp 5556 8083 8086 15000

# expose volumes
VOLUME ["/data", "/logs"]

ADD ./bootstrap.sh /bootstrap.sh
CMD /configure-and-start.sh
