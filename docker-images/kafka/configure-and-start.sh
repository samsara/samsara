#!/bin/bash

# log output of this script
exec > >( tee -a /logs/configure-and-start.out )
exec 2>&1

# REQUIRED:
#   KAFKA_BROKER_ID
#   ZOOKEEPER_PORT_2181_TCP_ADDR
export HOSTNAME=${HOSTNAME-kafka$KAFKA_BROKER_ID}
export IP=${ADV_IP:-`ip ro get 8.8.8.8 | grep -oP "(?<=src )(\S+)"`}
export KAFKA_ENABLE_REPORTING_STATSD=${KAFKA_ENABLE_REPORTING_STATSD-false}
export STATSD_PORT_8125_TCP_ADDR=${STATSD_PORT_8125_TCP_ADDR}
export KAFKA_BROKER_PORT=${KAFKA_BROKER_PORT-9092}


export CONFIG_FILE=/opt/kafka/config/server.properties
# replace variables in template with environment values
echo "TEMPLATE: generating configuation."
perl -pe 's/%%([A-Za-z0-9_]+)%%/defined $ENV{$1} ? $ENV{$1} : $&/eg' < ${CONFIG_FILE}.tmpl > $CONFIG_FILE

# check if all properties have been replaced
if grep -qoP '%%[^%]+%%' $CONFIG_FILE ; then
    echo "ERROR: Not all variable have been resolved,"
    echo "       please set the following variables in your environment:"
    grep -oP '%%[^%]+%%' $CONFIG_FILE | sed 's/%//g' | sort -u
    exit 1
fi


exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
