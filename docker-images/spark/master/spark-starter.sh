#!/bin/bash

. $(dirname $0)/conf/spark-env.sh

$(dirname $0)/bin/spark-class \
             org.apache.spark.deploy.master.Master \
             --ip $SPARK_MASTER_IP \
             --port $SPARK_MASTER_PORT \
             --webui-port $SPARK_MASTER_WEBUI_PORT
