#!/bin/bash

# REQUIRED:
#   ZOOKEEPER_PORT_2181_TCP_ADDR
export HOSTNAME=${HOSTNAME-els}
export IP=${ADV_IP:-`ip ro get 8.8.8.8 |  grep src | sed -E "s/.* src (\S+)/\1/g"`}
export ELS_NODE_TYPE=${ELS_NODE_TYPE:-standard}
export ELS_HEAP_SIZE=${ELS_HEAP_SIZE:-`free -g | grep Mem: | awk '{printf( "%.0fg\n", $2 / 2 + 1 )}'`}
export AWS_ACCESS_KEY=${AWS_ACCESS_KEY:-}
export AWS_SECRET_KEY=${AWS_SECRET_KEY:-}
export AWS_REGION=${AWS_REGION:-eu-west}
export AWS_REPOS_NAME=${AWS_REPOS_NAME:-samsara-backups}
export AWS_REPOS_ACCESS_KEY=${AWS_REPOS_ACCESS_KEY:-$AWS_ACCESS_KEY}
export AWS_REPOS_SECRET_KEY=${AWS_REPOS_SECRET_KEY:-$AWS_SECRET_KEY}
export AWS_REPOS_BASE_PATH=${AWS_REPOS_BASE_PATH:-/samsara}

export CONFIG_FILE=/opt/els/config/elasticsearch.yml
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


export CONFIG_FILE=/etc/supervisor/conf.d/els-supervisor.conf
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
