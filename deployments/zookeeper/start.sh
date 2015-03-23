#!/bin/bash

# add services for client, peer, and election connections
kubectl.sh create -f $(dirname $0)/zookeeper-client-service.yaml

seq 3 | xargs -I {} kubectl.sh create -f $(dirname $0)/zookeeper-peer-service-{}.yaml
seq 3 | xargs -I {} kubectl.sh create -f $(dirname $0)/zookeeper-election-service-{}.yaml

# now add controllers
seq 3 | xargs -I {} kubectl.sh create -f $(dirname $0)/zookeeper-ctrl-{}.yaml
