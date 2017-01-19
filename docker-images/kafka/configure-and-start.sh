#!/bin/bash

# log output of this script
exec > >( tee -a /logs/configure-and-start.out )
exec 2>&1

# REQUIRED:
#   KAFKA_BROKER_ID
#   ZOOKEEPER_PORT_2181_TCP_ADDR
export HOSTNAME=${HOSTNAME-kafka$KAFKA_BROKER_ID}
export IP=${ADV_IP:-`ip ro get 8.8.8.8 |  grep src | sed -E "s/.* src (\S+)/\1/g"`}
if [ "$KAFKA_BROKER_PROTOCOL_VERSION" != "" ] ; then
   export KAFKA_BROKER_PROTOCOL_VERSION_LINE="inter.broker.protocol.version=$KAFKA_BROKER_PROTOCOL_VERSION"
fi

if [ "$KAFKA_MESSAGE_FORMAT_VERSION" != "" ] ; then
    export KAFKA_MESSAGE_FORMAT_VERSION_LINE="log.message.format.version=$KAFKA_MESSAGE_FORMAT_VERSION"
fi

if [ "$KAFKA_BROKER_RACK" != "" ] ; then
    export KAFKA_BROKER_RACK_LINE="broker.rack=$KAFKA_BROKER_RACK"
fi

# Configure server
synapse /opt/kafka/config/server.properties.tmpl \
        /etc/supervisor/conf.d/kafka-supervisor.conf.tmpl

exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
