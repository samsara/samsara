#!/bin/bash

if [ "$(id -u)" != "0" ] ; then
    echo "Running the script with root privilege's"
    sudo "$0"
    exit $?
fi

function pull-all(){
    echo '------------------------------------------------------------------'
    echo "                  pull Samsara's images"
    echo '------------------------------------------------------------------'
    docker pull samsara/ingestion-api && \
        docker pull samsara/samsara-core  && \
        docker pull samsara/kibana        && \
        docker pull samsara/elasticsearch && \
        docker pull samsara/qanal         && \
        docker pull samsara/monitoring    && \
        docker pull samsara/kafka         && \
        docker pull samsara/zookeeper
}


#
# Docker seems to fail during the pull with the following error:
#
# Error pulling image (latest) from aaaaaa/bbbbb, Server error: 404 trying to fetch remote history for zzzzzzz
#
# the image is different every time, and it is fixed by retrying
# It seems a transient error of the registry
#

n=0
until [ $n -ge 10 ]
do
    pull-all && break
    n=$[$n+1]
    echo '------------------------------------------------------------------'
    echo "                           RETRYING: $n"
    echo '------------------------------------------------------------------'
    sudo service docker stop
    sleep 15
    sudo service docker start
    sleep 15
done
