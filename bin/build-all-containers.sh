#!/bin/bash -e
#
# Builds all the containers using the given version
#
# Usage:
#
#    build-all-containers.sh snapshot
#
# NOTE: if the version is not passed it assumes 'samsara.version'
# to pass options to docker you can use $DOCKER_OPTS like:
#
#    DOCKER_OPTS=--no-cache build-all-containers.sh
#


BASE=$(cd $(dirname $0)/../docker-images && pwd)
SAM=$BASE/..
VER=${1:-`cat $BASE/../samsara.version`}
[ `uname` == "Darwin" ] && export SED=gsed || export SED=sed


function docker-build(){
    # $1 - dir
    # $2 - tag
    # $3 - version
    # $4 - OPT transformation

    export DF="Dockerfile.temp.$RANDOM"

    echo ''
    echo ''
    echo '------------------------------------------------------------------'
    echo '              ' $2:$3
    echo '------------------------------------------------------------------'
    echo "using: $DF ($DOCKER_OPTS)"
    echo '------------------------------------------------------------------'
    cd $1
    rm -f Dockerfile.temp*
    $SED "${4:-/^FROM samsara\/.*/s/:.*$/:$3/g}" Dockerfile > $DF
    ln -fs $DF Dockerfile.temp
    docker build $DOCKER_OPTS -t $2:$3 -f $DF .
    rm -f $DF Dockerfile.temp*
}


# build third-party
docker-build $BASE/base         samsara/base-image-jdk8 $VER
docker-build $BASE/zookeeper    samsara/zookeeper       $VER
docker-build $BASE/kafka        samsara/kafka           $VER
docker-build $BASE/els          samsara/elasticsearch   $VER
docker-build $BASE/kibana       samsara/kibana          $VER
docker-build $BASE/monitoring   samsara/monitoring      $VER
#docker-build $BASE/spark/master samsara/spark-master    $VER
#docker-build $BASE/spark/worker samsara/spark-worker    $VER

# build internal
docker-build $SAM/ingestion-api samsara/ingestion-api   $VER
docker-build $SAM/core          samsara/samsara-core    $VER
docker-build $SAM/qanal         samsara/qanal           $VER
