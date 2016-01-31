#!/bin/bash


# REQUIRED:
#   ZOOKEEPER_PORT_2181_TCP
# OR
#   ZOOKEEPER_1_PORT_2181_TCP
#
#   KAFKA_TOPICS_SPEC
#   ELS_PORT_9200_TCP_ADDR

export KAFKA_ZOOKEEPER_CONNECT=$(env | grep 'ZOOKEEPER.*_PORT_2181_TCP=' | sed -e 's|.*tcp://||' | sort -u | tr "\n" "," | sed 's/,$//g')
if [[ -n "${KAFKA_ZOOKEEPER_CONNECT}" ]]; then
    export REPLACE_ZOOKEEPER_CONNECT="s/:zookeeper-connect\s.+$/:zookeeper-connect \"${KAFKA_ZOOKEEPER_CONNECT}\"/"
fi

# setting defaults
export HOSTNAME=${HOSTNAME:-qanal}
export TRACKING_ENABLED=${TRACKING_ENABLED:-false}
export ELS_PORT_9200_TCP_PORT=${ELS_PORT_9200_TCP_PORT:-9200}
export RIEMANN_PORT_5555_TCP_ADDR=${RIEMANN_PORT_5555_TCP_ADDR:-localhost}
export RIEMANN_PORT_5555_TCP_PORT=${RIEMANN_PORT_5555_TCP_PORT:-5555}


export CONFIG_FILE=/opt/qanal/conf/config.edn
# replace variables in template with environment values
echo "TEMPLATE: generating configuation."
perl -pe 's/%%([A-Za-z0-9_]+)%%/defined $ENV{$1} ? $ENV{$1} : $&/eg' < ${CONFIG_FILE}.tmpl > $CONFIG_FILE

# check if all properties have been replaced
if grep -qoE '%%[^%]+%%' $CONFIG_FILE ; then
    echo "ERROR: Not all variable have been resolved,"
    echo "       please set the following variables in your environment:"
    grep -oE '%%[^%]+%%' $CONFIG_FILE | sed 's/%//g' | sort -u
    exit 1
fi


/usr/bin/supervisord -c /etc/supervisor/supervisord.conf
