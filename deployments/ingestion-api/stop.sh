#!/bin/bash

# add services connections
kubectl.sh delete -f $(dirname $0)/ingestion-api-service.yaml

# now add controller
kubectl.sh resize rc ingestion-api --replicas=0
sleep 10
kubectl.sh delete -f $(dirname $0)/ingestion-api-ctrl.yaml
