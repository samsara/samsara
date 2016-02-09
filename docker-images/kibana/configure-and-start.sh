#!/bin/bash

# REQUIRED:
#   ELASTICSEARCH_PORT_9200_TCP_ADDR
#   ELASTICSEARCH_PORT_9200_TCP_PORT
synapse /opt/kibana/config/kibana.yml.tmpl

exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
