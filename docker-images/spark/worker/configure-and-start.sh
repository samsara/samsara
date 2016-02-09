#!/bin/bash

# log output of this script
exec > >( tee -a /logs/configure-and-start.out )
exec 2>&1

# REQUIRED: (one or more)
#   SPARK_MASTER_PORT_7077_TCP_ADDR
#   SPARK_MASTER_PORT_7077_TCP_PORT

export HOSTNAME=${HOSTNAME:-spark-$HOSTNAME}
export IP=${ADV_IP:-`ip ro get 8.8.8.8 |  grep src | sed -E "s/.* src (\S+)/\1/g"`}

synapse /opt/spark/conf/spark-env.sh.tmpl
chmod +x /opt/spark/conf/spark-env.sh

exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
