#!/bin/bash

# log output of this script
exec > >( tee -a /logs/configure-and-start.out )
exec 2>&1

# REQUIRED:
#   ZOOKEEPER_PORT_2181_TCP_ADDR
export HOSTNAME=${HOSTNAME:-spark-$HOSTNAME}
export IP=${ADV_IP:-`ip ro get 8.8.8.8 |  grep src | sed -E "s/.* src (\S+)/\1/g"`}

synapse /opt/spark/conf/spark-env.sh.tmpl
chmod +x /opt/spark/conf/spark-env.sh

exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
