#!/bin/bash -e
#
# Starts the Samsara's cluster using docker-compose.
#
# Usage:
#
#    start-samsara.sh [[version] bootstrap-ver]
#
# NOTE: if the version is not passed it assumes `latest`
#       if bootstrap-ver is not passed it assume `master` (branch)


BASE=$(cd $(dirname $0)/../docker-images && pwd)
SAM=$BASE/..
VER=${1:-latest}
BVER=${2:-master}
[ `uname` == "Darwin" ] && export SED=sed || export SED=gsed


echo ''
echo ''
echo '------------------------------------------------------------------'
echo "           Starting Samsara's dev and test cluster"
echo '------------------------------------------------------------------'
echo "container version: $VER"
echo "bootstrap version: $BVER"
echo '------------------------------------------------------------------'

# patch the containers version and the bootstrap script version
cd $BASE
$SED "/image:/s/$/:$VER/g;/command:.*bootstrap.sh/s/master/$BVER/g" docker-compose.yml > docker-compose.yml.$VER
$SED "/image:/s/$/:$VER/g;/command:.*bootstrap.sh/s/master/$BVER/g" docker-compose-with-spark.yml > docker-compose-with-spark.yml.$VER

# start the cluster up
docker-compose -f docker-compose.yml.$VER up
