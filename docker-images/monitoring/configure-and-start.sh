#!/bin/bash

# REQUIRED:
#   ELASTICSEARCH_PORT_9200_TCP_ADDR
#   ELASTICSEARCH_PORT_9200_TCP_PORT
export HTTP_USER=${HTTP_USER:-admin}
export HTTP_PASS=${HTTP_PASS:-samsara}


# set grafana password
echo ${HTTP_PASS} | htpasswd -i -c /opt/grafana/.htpasswd  ${HTTP_USER}

exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
