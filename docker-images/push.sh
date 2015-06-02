#!/bin/bash

#
# PUSHING BASE IMAGE
#
docker push samsara/base-image-jdk7:latest
docker push samsara/base-image-jdk7:u1410-j7u75
docker push samsara/base-image-jdk8:latest
docker push samsara/base-image-jdk8:u1410-j8u40


#
# PUSHING zookeeper image
#
docker push samsara/zookeeper:latest
docker push samsara/zookeeper:3.4.6


#
# PUSHING Kafka image
#
docker push samsara/kafka:latest
docker push samsara/kafka:0.8.2.1


#
# PUSHING ElasticSearch image
#
docker push samsara/elasticsearch:latest
docker push samsara/elasticsearch:1.5.2


#
# PUSHING Kibana image
#
docker push samsara/kibana:latest
docker push samsara/kibana:4.0.2
