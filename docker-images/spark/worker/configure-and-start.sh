#!/bin/bash

# log output of this script
exec > >( tee -a /logs/configure-and-start.out )
exec 2>&1

# REQUIRED:
#   SPARK_MASTER_PORT_7077_TCP_ADDR
#   SPARK_MASTER_PORT_7077_TCP_PORT
#   or
#   SPARK_MASTERS

export HOSTNAME=${HOSTNAME:-spark-$HOSTNAME}
export IP=${ADV_IP:-`ip ro get 8.8.8.8 |  grep src | sed -E "s/.* src (\S+)/\1/g"`}

export SPARK_MASTER_PORT_7077_TCP_PORT=${SPARK_MASTER_PORT_7077_TCP_PORT:-7077}
export SPARK_WORKER_PORT=${SPARK_WORKER_PORT:-7078}
export SPARK_WORKER_WEBUI_PORT=${SPARK_WORKER_WEBUI_PORT:-8081}
export SPARK_LOCAL_DIRS=/data
export SPARK_WORKER_DIR=/data

export SPARK_MASTERS=${SPARK_MASTERS:-$SPARK_MASTER_PORT_7077_TCP_ADDR:$SPARK_MASTER_PORT_7077_TCP_PORT}

export CONFIG_FILE=/opt/spark/conf/spark-env.sh
# replace variables in template with environment values
echo "TEMPLATE: generating configuation."
perl -pe 's/%%([A-Za-z0-9_]+)%%/defined $ENV{$1} ? $ENV{$1} : $&/eg' < ${CONFIG_FILE}.tmpl > $CONFIG_FILE

chmod +x $CONFIG_FILE

# check if all properties have been replaced
if grep -qoE '%%[^%]+%%' $CONFIG_FILE ; then
    echo "ERROR: Not all variable have been resolved,"
    echo "       please set the following variables in your environment:"
    grep -oE '%%[^%]+%%' $CONFIG_FILE | sed 's/%//g' | sort -u
    exit 1
fi


exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
