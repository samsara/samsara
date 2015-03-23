#!/bin/bash

# add services for client, peer, and election connections
kubectl.sh delete -f $(dirname $0)/zookeeper-client-service.yaml

seq 3 | xargs -I {} kubectl.sh delete -f $(dirname $0)/zookeeper-peer-service-{}.yaml
seq 3 | xargs -I {} kubectl.sh delete -f $(dirname $0)/zookeeper-election-service-{}.yaml

# now delete controllers
seq 3 | xargs -I {} kubectl.sh resize rc zookeeper-{} --replicas=0
seq 3 | xargs -I {} kubectl.sh delete -f $(dirname $0)/zookeeper-ctrl-{}.yaml
