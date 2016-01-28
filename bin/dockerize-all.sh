#!/bin/bash -e
#
# dockerize-all.sh builds and pushes docker images for samsara apps


export CUR=$(pwd)
export BASE=`cd $(dirname $0)/.. && pwd`

function dockerize(){
    # $1 - dir
    # $2 - repo_name
    # $3 - tag

    echo '--------------------------------------------------------------------------------'
    echo '              BUILIDING AND PUSHING DOCKER IMAGE  :' $2:$3
    echo '--------------------------------------------------------------------------------'

    cd $1

    docker build -t $2:$3 .
    docker login -e $DOCKER_EMAIL -u $DOCKER_USER -p $DOCKER_PASS
    docker push $2:$3

    cd - 
}


## build and push the internal containers
dockerize  ingestion-api samsara/ingestion-api   snapshot
dockerize  qanal         samsara/qanal           snapshot
dockerize  core          samsara/samsara-core    snapshot
