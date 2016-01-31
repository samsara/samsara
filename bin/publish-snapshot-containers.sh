#!/bin/bash
#
# it pushes the snapshot containers to docker hub
#


[ "$1" == "CI" ] && docker login -e $DOCKER_EMAIL -u $DOCKER_USER -p $DOCKER_PASS

function docker-push(){
    ATTEMPT=0
    until docker push $1 || [ $ATTEMPT -ge 4 ]; do
        export ATTEMPT=$(($ATTEMPT + 1))
        echo "Failed to push $1, retrying..."
        sleep 10
    done
    [ $ATTEMPT -ge 4 ] && echo "ERROR: Failed to push $1, giving up." && exit 1
    return 0
}


# Flock seems not helping as we are only doing
# one push at the time
#function docker-push(){
#    flock -x /var/lock/docker-push docker push $1
#}


# push internal containers
docker-push samsara/ingestion-api:snapshot
docker-push samsara/samsara-core:snapshot
docker-push samsara/qanal:snapshot


# push third-party containers
docker-push samsara/base-image-jdk7:snapshot
docker-push samsara/base-image-jdk8:snapshot
docker-push samsara/zookeeper:snapshot
docker-push samsara/kafka:snapshot
docker-push samsara/elasticsearch:snapshot
docker-push samsara/kibana:snapshot
docker-push samsara/monitoring:snapshot
docker-push samsara/spark-master:snapshot
docker-push samsara/spark-worker:snapshot
