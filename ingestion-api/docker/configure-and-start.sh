#!/bin/bash

# REQUIRED:
#   KAFKA_PORT_9092_TCP_ADDR
# OR
#   KAFKA_1_PORT_9092_TCP_ADDR
export HOSTNAME=${HOSTNAME:-"inegstion"}
export OUTPUT_TOPIC=${OUTPUT_TOPIC:-"ingestion"}
export TRACKING_ENABLED=${TRACKING_ENABLED:-false}
export RIEMANN_PORT_5555_TCP_ADDR=${RIEMANN_PORT_5555_TCP_ADDR:-localhost}
export RIEMANN_PORT_5555_TCP_PORT=${RIEMANN_PORT_5555_TCP_PORT:-5555}


export CONFIG_FILE=/opt/ingestion-api/config/config.edn
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
