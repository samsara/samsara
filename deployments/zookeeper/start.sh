#!/bin/bash

# now add controllers
seq 3 | xargs -I {} kubectl.sh create -f $(dirname $0)/zookeeper-ctrl-{}.yaml

sleep 5

# add services for client, peer, and election connections
kubectl.sh create -f $(dirname $0)/zookeeper-client-service.yaml
