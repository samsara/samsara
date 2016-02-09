#!/bin/bash


# REQUIRED:
#   ZOOKEEPER_PORT_2181_TCP (one or more)
#   KAFKA_TOPICS_SPEC
#   ELS_PORT_9200_TCP_ADDR

synapse /opt/qanal/conf/config.edn.tmpl

/usr/bin/supervisord -c /etc/supervisor/supervisord.conf
