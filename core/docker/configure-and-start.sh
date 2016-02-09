#!/bin/bash

# REQUIRED: (one or more)
#   KAFKA_PORT_9092_TCP_ADDR
#   ZOOKEEPER_PORT_2181_TCP_ADDR

export SINGLE_BROKER=${SINGLE_BROKER:-false}
export SAMZA_CONFIG=${SAMZA_CONFIG:-""}
[ "$SINGLE_BROKER" = "true" ] && export SAMZA_CONFIG="$SAMZA_CONFIG :task.checkpoint.replication.factor 1"

synapse /opt/samsara-core/config/config.edn.tmpl

exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
