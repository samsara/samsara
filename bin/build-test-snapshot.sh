#!/bin/bash -e
#
# Builds all the projects and all the containers using the latest code.
# then it performs a sanity test of the entire cluster.
#

SAM=$(cd $(dirname $0)/.. && pwd)
VER=${1:-snapshot}

echo '------------------------------------------------------------------'
echo '              BUILDING SAMSARA ' $VER
echo '------------------------------------------------------------------'


# build all projects
MIDJE_COLORIZE=NONE $(dirname $0)/build-all-projects.sh -test

# now build the containers using the latest code
DOCKER_OPTS=--no-cache $(dirname $0)/build-all-containers.sh $VER

# now start a cluster using the containers just created
$(dirname $0)/start-samsara.sh $VER &

# wait for the containers to be created
echo 'wait for the containers to be created'
sleep 120

# run a sanity check
$(dirname $0)/cluster-sanity-check.sh
TEST=$?

# kill cluster
docker-compose -f $SAM/docker-images/docker-compose.yml.$VER kill

# exit code
exit $TEST
