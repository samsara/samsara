#!/bin/bash

# REQUIRED:
#   KAFKA_BROKER_ID
#   ZOOKEEPER_PORT_2181_TCP_ADDR
export HOSTNAME=${HOSTNAME-kafka$KAFKA_BROKER_ID}
export KAFKA_ENABLE_REPORTING_STATSD=${KAFKA_ENABLE_REPORTING_STATSD-false}
export STATSD_PORT_8125_TCP_ADDR=${STATSD_PORT_8125_TCP_ADDR}


# replace variables in template with environment values
echo "TEMPLATE: generating configuation."
perl -pe 's/%%([A-Za-z0-9_]+)%%/defined $ENV{$1} ? $ENV{$1} : $&/eg' < /opt/kafka/config/server.properties.tmpl > /opt/kafka/config/server.properties

# check if all properties have been replaced
if grep -qoP '%%[^%]+%%' /opt/kafka/config/server.properties ; then
    echo "ERROR: Not all variable have been resolved,"
    echo "       please set the following variables in your environment:"
    grep -oP '%%[^%]+%%' /opt/kafka/config/server.properties | sed 's/%//g' | sort -u
    exit 1
fi


exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
