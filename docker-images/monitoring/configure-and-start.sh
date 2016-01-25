#!/bin/bash

# REQUIRED:
#   ELASTICSEARCH_PORT_9200_TCP_ADDR
#   ELASTICSEARCH_PORT_9200_TCP_PORT
export HTTP_USER=${HTTP_USER:-admin}
export HTTP_PASS=${HTTP_PASS:-samsara}

export CONFIG_FILE=/opt/grafana/conf/custom.ini
# replace variables in template with environment values
echo "TEMPLATE: generating configuation."
perl -pe 's/%%([A-Za-z0-9_]+)%%/defined $ENV{$1} ? $ENV{$1} : $&/eg' < ${CONFIG_FILE}.tmpl > $CONFIG_FILE

# check if all properties have been replaced
if grep -qoP '%%[^%]+%%' $CONFIG_FILE ; then
    echo "ERROR: Not all variable have been resolved,"
    echo "       please set the following variables in your environment:"
    grep -oP '%%[^%]+%%' $CONFIG_FILE | sed 's/%//g' | sort -u
    exit 1
fi


# start the influxdb boostrap in background
/bootstrap.sh "$HTTP_USER" "$HTTP_PASS" &

exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
