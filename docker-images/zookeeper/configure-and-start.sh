#!/bin/bash

# log output of this script
exec > >( tee -a /logs/configure-and-start.out )
exec 2>&1

#
# it uses Kubernetes API to find the list of all zookeepers
#
export DISCOVERY_SELECTOR=${DISCOVERY_SELECTOR:-zookeeper}
export DISCOVERY_ENSEMBLE_MIN_SIZE=${DISCOVERY_ENSEMBLE_MIN_SIZE:-3}

function discover-ensemble(){
    #
    # $1 - required, DISCOVERY_SELECTOR, label to use to discover the ensemble
    #
    curl -s "http://${KUBERNETES_RO_SERVICE_HOST}:${KUBERNETES_RO_SERVICE_PORT}/api/v1beta1/pods" | jq ".items | map( select( .labels.name == \"${1}\") ) |  map( { ip: .currentState.podIP, ports: (.desiredState.manifest.containers[0].ports | map ( {(.name): .containerPort }) | add) } )"
}


function discover-ensemble-as-list(){
    #
    # $1 - required, DISCOVERY_SELECTOR, label to use to discover the ensemble
    #
    discover-ensemble $1 | jq -r '.[]|.ip' | grep -v null
}


function discover-ensemble-as-csv(){
    #
    # $1 - required, DISCOVERY_SELECTOR, label to use to discover the ensemble
    #
    discover-ensemble-as-list $1 | paste -sd "," -
}



if [ "$ZK_SERVER_ID" == "" ] ; then
    echo "ERROR: \$ZK_SERVER_ID not defined or empty"
    exit 1
fi

# if running in kubernetes then start autodiscovery
if [ -n "$KUBERNETES_RO_SERVICE_HOST" ] ; then
    while [ $(discover-ensemble-as-list $DISCOVERY_SELECTOR | wc -l) -lt $DISCOVERY_ENSEMBLE_MIN_SIZE ] ; do
        echo "currently found: " $( discover-ensemble-as-csv $DISCOVERY_SELECTOR )
        echo "Ensamble too small.. waiting for more members to come online..."
        sleep 5
    done
fi

function get-server-list(){
    # if running in Kubernetes then use the service discovery
    if [ -n "$KUBERNETES_RO_SERVICE_HOST" ] ; then
        curl -s "http://${KUBERNETES_RO_SERVICE_HOST}:${KUBERNETES_RO_SERVICE_PORT}/api/v1beta1/pods" | jq -r ".items | map( select( .labels.name == \"${1}\") ) | map(  [.labels.server, \":\", .currentState.podIP] | add )[]" | sed 's/^/server./g;s/$/:2888:3888/g'
    else
        # this server
        echo "server.${ZK_SERVER_ID}="$(ip ro get 8.8.8.8 | grep -oP "(?<=src )(\S+)")":2888:3888"
        # and all others
        env | grep 'ZOOKEEPER_.*_PORT_2181_TCP=' | sed -r 's/ZOOKEEPER.*([0-9]+)_PORT_2181_TCP=tcp:\/\/(.*):[0-9]+/server.\1=\2:2888:3888/g' | grep -v "server.${ZK_SERVER_ID}"
    fi
}


#
# configuring zookeeper
#

cat <<EOF > /opt/zookeeper/conf/zoo.cfg
tickTime=2000
dataDir=/data
clientPort=2181
initLimit=10
syncLimit=2
$( get-server-list $DISCOVERY_SELECTOR )
EOF

echo "$ZK_SERVER_ID" > /data/myid

#
# now it's ready to start
#
exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf
