#!/bin/bash

# REQUIRED:
#   ELASTICSEARCH_PORT_9200_TCP_ADDR
#   ELASTICSEARCH_PORT_9200_TCP_PORT
export KIBANA_PORT=${KIBANA_PORT:-8000}
export KIBANA_INDEX=${KIBANA_INDEX:-kibana}

export CONFIG_FILE=/opt/kibana/config/kibana.yml
# replace variables in template with environment values
echo "TEMPLATE: generating configuation."
perl -pe 's/%%([A-Za-z0-9_]+)%%/defined $ENV{$1} ? $ENV{$1} : $&/eg' < ${CONFIG_FILE}.tmpl > $CONFIG_FILE

# check if all properties have been replaced
if grep -qoE '%%[^%]+%%' $CONFIG_FILE ; then
    echo "ERROR: Not all variable have been resolved,"
    echo "       please set the following variables in your environment:"
    grep -oE '%%[^%]+%%' $CONFIG_FILE | sed 's/%//g' | sort -u
    exit 1
fi


exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
