#!/bin/bash


export KAFKA_ZOOKEEPER_CONNECT=$(env | grep ZK.*PORT_2181_TCP= | sed -e 's|.*tcp://||' | paste -sd ,)
if [[ -n "${KAFKA_ZOOKEEPER_CONNECT}" ]]; then
    export REPLACE_ZOOKEEPER_CONNECT="s/:zookeeper-connect\s.+$/:zookeeper-connect \"${KAFKA_ZOOKEEPER_CONNECT}\"/"
fi

export ELS_ENDPOINT=$(echo ${ELS_PORT_9200_TCP} | sed -e 's|.*tcp://||')
if [[ -n "${ELS_ENDPOINT}" ]]; then
    export REPLACE_ELS_ENDPOINT="s/:end-point\s.+}$/:end-point \"http:\/\/${ELS_ENDPOINT}\"}/"
fi

if [[ -n "${KAFKA_TOPIC}" ]]; then
    export REPLACE_KAFKA_TOPIC="s/:topic\s.+$/:topic \"${KAFKA_TOPIC}\"/"
fi

if [[ -n "${KAFKA_PARTITION_ID}" ]]; then
    export REPLACE_KAFKA_PARTITION_ID="s/:partition-id\s.+$/:partition-id ${KAFKA_PARTITION_ID}/"
fi

if [[ ! -e /opt/qanal/conf/config.edn ]]; then
    cat /opt/qanal/conf/config.edn.orig | sed -r "${REPLACE_ZOOKEEPER_CONNECT}" | sed -r "${REPLACE_ELS_ENDPOINT}" | sed -r "${REPLACE_KAFKA_TOPIC}" | sed -r "${REPLACE_KAFKA_PARTITION}" > /opt/qanal/conf/config.edn
fi

java -jar /opt/qanal/qanal.jar -c /opt/qanal/conf/config.edn
