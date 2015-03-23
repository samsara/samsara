#!/bin/bash

# add services for client, peer, and election connections
kubectl.sh delete -f $(dirname $0)/kafka-service.yaml

# now delete controllers
seq 3 | xargs -I {} kubectl.sh resize rc kafka-{} --replicas=0
sleep 10

seq 3 | xargs -I {} kubectl.sh delete -f $(dirname $0)/kafka-ctrl-{}.yaml
