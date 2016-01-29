#!/bin/bash -e
#
# it pushes the snapshot containers to docker hub
#


[ "$1" == "CI" ] && docker login -e $DOCKER_EMAIL -u $DOCKER_USER -p $DOCKER_PASS


# push third-party containers
docker push samsara/base-image-jdk7:snapshot
docker push samsara/base-image-jdk8:snapshot
docker push samsara/zookeeper:snapshot
docker push samsara/kafka:snapshot
docker push samsara/elasticsearch:snapshot
docker push samsara/kibana:snapshot
docker push samsara/monitoring:snapshot
docker push samsara/spark-master:snapshot
docker push samsara/spark-worker:snapshot

# push internal containers
docker push samsara/ingestion-api:snapshot
docker push samsara/samsara-core:snapshot
docker push samsara/qanal:snapshot
