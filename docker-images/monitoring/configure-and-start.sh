#!/bin/bash

export HTTP_USER=${HTTP_USER:-admin}
export HTTP_PASS=${HTTP_PASS:-samsara}

synapse /opt/grafana/conf/custom.ini.tmpl

# start the influxdb boostrap in background
/bootstrap.sh "$HTTP_USER" "$HTTP_PASS" &

exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
