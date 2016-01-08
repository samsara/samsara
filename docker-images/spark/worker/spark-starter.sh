#!/bin/bash

. $(dirname $0)/conf/spark-env.sh

$(dirname $0)/bin/spark-class \
             org.apache.spark.deploy.worker.Worker \
             --work-dir $SPARK_WORKER_DIR \
             --host $SPARK_LOCAL_IP \
             --port $SPARK_WORKER_PORT \
             --webui-port $SPARK_WORKER_WEBUI_PORT \
             spark://$SPARK_MASTERS
