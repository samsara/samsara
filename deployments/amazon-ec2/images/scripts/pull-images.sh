#!/bin/bash -e

if [ "$(id -u)" != "0" ] ; then
    echo "Running the script with root privilege's"
    sudo "$0"
    exit $?
fi


echo '------------------------------------------------------------------'
echo "                  pull Samsara's images"
echo '------------------------------------------------------------------'
docker pull samsara/ingestion-api
docker pull samsara/samsara-core
docker pull samsara/kibana
docker pull samsara/elasticsearch
docker pull samsara/qanal
docker pull samsara/monitoring
docker pull samsara/kafka
docker pull samsara/zookeeper
