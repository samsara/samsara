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
    if [ `uname` == "Darwin" ] ; then
        echo $DOCKER_HOST | grep -oE '[0-9]+(\.[0-9]+){3}'
    else
        CNTID=`docker ps | grep $1 | cut -d' ' -f1`
        docker inspect $CNTID | grep '"IPAddress"' | grep -oE '[0-9]+(\.[0-9]+){3}'
    fi
}

INGEST=$(container-ip ingestion_1):9000
ELS=$(container-ip elasticsearch_1):9200

echo "sending event..."
export NONCE=$RANDOM
cat <<EOF | curl -i -H "Content-Type: application/json" \
                 -H "X-Samsara-publishedTimestamp: $(date +%s999)" \
                 -XPOST "http://$INGEST/v1/events" -d @-
[
 {
 "timestamp": $(date +%s000),
 "sourceId": "samsara",
 "eventName": "sanity.check.started",
 "nonce": "$NONCE"
 }
]
EOF


echo "waiting for full processing to complete"
sleep 10

echo "check if event is present in the index"
curl -sS -XGET "http://$ELS/_all/events/_search?q=nonce:+$NONCE" \
    | grep -qoE '"hits":{"total":1,' && export TEST="OK"


echo ''
echo ''
echo '------------------------------------------------------------------'
echo -n "           Sanity check "
[ "$TEST" == "OK" ] && echo "PASSED" || echo "FAILED"
echo '------------------------------------------------------------------'
echo ''
echo ''


[ "$TEST" == "OK" ] && exit 0 || exit 1
