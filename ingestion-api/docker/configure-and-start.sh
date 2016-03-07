#!/bin/bash

# /opt/ingestion-api/config/config.edn
#
# KAFKA_PORT_9092_TCP_ADDR: REQUIRED: (one or more)
# RIEMANN_PORT_5555_TCP_ADDR: OPTIONAL

exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
