FROM samsara/base-image-jdk8:u1510-j8u71

MAINTAINER Samsara's team (https://github.com/samsara/samsara/docker-images)

#
# Riemann installation
#
RUN export RIEMANN_VERSION=0.2.10 && \
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
RUN export INFLUXDB_VERSION=0.9.6.1 && \
    wget --progress=dot:mega --no-check-certificate \
    https://s3.amazonaws.com/influxdb/influxdb_${INFLUXDB_VERSION}_amd64.deb -O /tmp/influxdb.deb && \
    dpkg -i /tmp/influxdb.deb && \
    rm /tmp/influxdb.deb

ADD ./influxdb-config.toml /opt/influxdb/config/config.toml
ADD ./influxdb-supervisor.conf /etc/supervisor/conf.d/influxdb-supervisor.conf


#
# MySQL installation
#
RUN \
    apt-get install -y libaio1 && \
    wget --progress=dot:mega --no-check-certificate \
    http://dev.mysql.com/get/Downloads/MySQL-5.7/mysql-5.7.10-linux-glibc2.5-x86_64.tar.gz -O - | \
    tar -zxvf - -C /opt && \
    ln -s /opt/mysql-* /opt/mysql

ADD ./mysql-start.sh /opt/mysql/bin/


#
# Grafana installation
#
RUN export GRAFANA_VERSION=2.6.0 && \
    wget --progress=dot:mega --no-check-certificate \
    https://grafanarel.s3.amazonaws.com/builds/grafana-2.6.0.linux-x64.tar.gz -O - \
    | tar -zxvf - -C /opt/ && \
    ln -s /opt/grafana-* /opt/grafana

ADD ./grafana-custom.ini.tmpl /opt/grafana/conf/custom.ini.tmpl
ADD ./grafana-supervisor.conf /etc/supervisor/conf.d/grafana-supervisor.conf
ADD ./mysql-supervisor.conf   /etc/supervisor/conf.d/mysql-supervisor.conf
ADD ./dashboards              /opt/grafana/dashboards

ADD ./configure-and-start.sh /configure-and-start.sh

# expose grafana, rieman ports,      influx,   supervisord
EXPOSE   80       5555 5555/udp 5556 8083 8086 15000

# expose volumes
VOLUME ["/data", "/logs"]

ADD ./bootstrap.sh /bootstrap.sh
CMD /configure-and-start.sh
