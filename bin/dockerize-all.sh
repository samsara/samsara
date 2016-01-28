#!/bin/bash -e
#
# dockerize-all.sh builds and pushes docker images for samsara apps


export CUR=$(pwd)
export BASE=`cd $(dirname $0)/.. && pwd`

function dockerize(){

    echo '--------------------------------------------------------------------------------'
    echo '              BUILIDING AND PUSHING DOCKER IMAGE FOR :' $*
    echo '--------------------------------------------------------------------------------'

    cd $BASE/$*

    docker build -t samsara/$*:snapshot .
    docker login -e $DOCKER_EMAIL -u $DOCKER_USER -p $DOCKER_PASS
    docker push samsara/$*:snapshot

    cd $CUR
}


## build ingestion-api docker image
dockerize ingestion-api


## build qanal docker image
dockerize qanal
