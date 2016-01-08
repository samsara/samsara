#!/bin/bash -e

BASE=$(dirname $0)

function build-and-tag() {

  WKD=$1
  CNT=$2
  VER=$3

  OLD=`pwd`
  cd $WKD

  echo "----------------------------------------------"
  echo "Building $CNT $VER"
  echo "----------------------------------------------"

  docker build --no-cache -t samsara/$CNT .
  img=$(docker images | grep samsara/$CNT | grep latest | awk '{print $3}')
  docker tag -f $img samsara/$CNT:$VER

  cd $OLD
}


#
# BUILDING images
#
sed -i '' 's/SJDK=\$SJDK./SJDK=\$SJDK7/g' $BASE/base/Dockerfile && \
build-and-tag $BASE/base       base-image-jdk7 "u1410-j7u75"

sed -i '' 's/SJDK=\$SJDK./SJDK=\$SJDK8/g' $BASE/base/Dockerfile && \
build-and-tag $BASE/base       base-image-jdk8 "u1410-j8u40"

build-and-tag $BASE/zookeeper  zookeeper       "3.4.6"
build-and-tag $BASE/kafka      kafka           "0.8.2.1"
build-and-tag $BASE/els        elasticsearch   "1.6.2"
build-and-tag $BASE/kibana     kibana          "4.0.2"
