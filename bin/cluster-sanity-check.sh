#!/bin/bash -e
#
# It sends a event via the ingestion api and it checks if the event comes
# out on the other side (ELS)
#
# Usage:
#
#    cluster-sanity-check.sh
#
#

BASE=$(cd $(dirname $0)/../docker-images && pwd)
SAM=$BASE/..
VER=${1:-latest}
BVER=${2:-master}
[ `uname` == "Darwin" ] && export SED=gsed || export SED=sed


echo ''
echo ''
echo '------------------------------------------------------------------'
echo "           Samsara's cluster sanity check"
echo '------------------------------------------------------------------'

function container-ip(){
    # if [ "$DOCKER_HOST" != "" ] ; then
    #     echo $DOCKER_HOST | grep -oE '[0-9]+(\.[0-9]+){3}'
    # else
    #     CNTID=`docker ps | grep $1 | cut -d' ' -f1`
    #     docker inspect $CNTID | grep '"IPAddress"' | head -1 | grep -oE '[0-9]+(\.[0-9]+){3}'
    # fi
    echo ${DOCKER_HOST:-127.0.0.1} | grep -oE '[0-9]+(\.[0-9]+){3}'
}

function wait_for {
    echo "Checking if $1 is started."

    while [ "$(nc -z -w 5 $2 $3 || echo 1)" == "1" ] ; do
        echo "Waiting for $1 to start up..."
        sleep 3
    done
}

INGEST=$(container-ip ingestion_1)
ELS=$(container-ip elasticsearch_1)

wait_for ingestion-api $INGEST 9000
wait_for elasticsearch $ELS    9200


echo ''
echo ''
echo "sending event..."
export NONCE=$RANDOM
cat <<EOF | curl -i -H "Content-Type: application/json" \
                 -H "X-Samsara-publishedTimestamp: $(date +%s999)" \
                 -XPOST "http://$INGEST:9000/v1/events" -d @-
[
 {
 "timestamp": $(date +%s000),
 "sourceId": "samsara",
 "eventName": "sanity.check.started",
 "nonce": "$NONCE"
 }
]
EOF


export ATTEMPT=0
export TEST=''
while [ "$TEST" == "" -a $ATTEMPT -le 10 ] ; do
    export ATTEMPT=$(($ATTEMPT + 1))
    echo "Waiting for event to appear: attempt $ATTEMPT"
    curl -sS -XGET "http://$ELS:9200/_all/events/_search?q=nonce:+$NONCE" \
        | grep -qoE '"hits":{"total":1,' && export TEST="OK"
    sleep 3
done

echo ''
echo ''
echo '------------------------------------------------------------------'
echo -n "           Sanity check "
[ "$TEST" == "OK" ] && echo "PASSED" || echo "FAILED"
echo '------------------------------------------------------------------'
echo ''
echo ''


[ "$TEST" == "OK" ] && exit 0 || exit 1
