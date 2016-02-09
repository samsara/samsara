#!/bin/bash

# REQUIRED:
#   ZOOKEEPER_PORT_2181_TCP_ADDR
export HOSTNAME=${HOSTNAME-els}
export IP=${ADV_IP:-`ip ro get 8.8.8.8 |  grep src | sed -E "s/.* src (\S+)/\1/g"`}
export ELS_HEAP_SIZE=${ELS_HEAP_SIZE:-`free -g | grep Mem: | awk '{printf( "%.0fg\n", $2 / 2 + 1 )}'`}

export AWS_ACCESS_KEY=${AWS_ACCESS_KEY:-}
export AWS_SECRET_KEY=${AWS_SECRET_KEY:-}
export AWS_REPOS_ACCESS_KEY=${AWS_REPOS_ACCESS_KEY:-$AWS_ACCESS_KEY}
export AWS_REPOS_SECRET_KEY=${AWS_REPOS_SECRET_KEY:-$AWS_SECRET_KEY}

synapse /opt/els/config/elasticsearch.yml.tmpl \
        /etc/supervisor/conf.d/els-supervisor.conf.tmpl

exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
