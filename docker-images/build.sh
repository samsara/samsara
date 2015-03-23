#!/bin/bash

BASE=$(dirname $0)

#
# BUILDIND BASE IMAGE
#
cd $BASE/base

sed -i '' 's/ENV SJDK .*/ENV SJDK  $SJDK7/g' Dockerfile
docker build --no-cache -t samsara/base-image-jdk7 .
img=$(docker images | grep samsara/base-image-jdk7 | grep latest | awk '{print $3}')
docker tag $img samsara/base-image-jdk7:u1410-j7u75

sed -i '' 's/ENV SJDK .*/ENV SJDK  $SJDK8/g' Dockerfile
docker build -t samsara/base-image-jdk8 .
img=$(docker images | grep samsara/base-image-jdk8 | grep latest | awk '{print $3}')
docker tag $img samsara/base-image-jdk8:u1410-j8u40


docker push samsara/base-image-jdk7:latest
docker push samsara/base-image-jdk7:u1410-j7u75
docker push samsara/base-image-jdk8:latest
docker push samsara/base-image-jdk8:u1410-j8u40


#
# BUILDIND zookeeper image
#
cd $BASE/zookeeper

docker build --no-cache -t samsara/zookeeper .
img=$(docker images | grep samsara/zookeeper | grep latest | awk '{print $3}')
docker tag $img samsara/zookeeper:3.4.6

docker push samsara/zookeeper:latest
docker push samsara/zookeeper:3.4.6


#
# BUILDIND Kafka image
#
cd $BASE/kafka

docker build --no-cache -t samsara/kafka .
img=$(docker images | grep samsara/kafka | grep latest | awk '{print $3}')
docker tag $img samsara/kafka:0.8.2.1

docker push samsara/kafka:latest
docker push samsara/kafka:0.8.2.1
