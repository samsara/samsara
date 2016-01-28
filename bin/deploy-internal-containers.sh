#!/bin/bash -e
#
# deploy-internal-containers.sh builds and deploys internal containers
#                               to docker hub


export CUR=$(pwd)
export BASE=`cd $(dirname $0)/.. && pwd`

function docker-deploy(){
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
docker-deploy  ingestion-api samsara/ingestion-api   snapshot
docker-deploy  qanal         samsara/qanal           snapshot
docker-deploy  core          samsara/samsara-core    snapshot
