#!/bin/bash

# REQUIRED: (one or more)
#   KAFKA_PORT_9092_TCP_ADDR

synapse /opt/ingestion-api/config/config.edn.tmpl

exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
