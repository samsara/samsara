#!/bin/bash


# now add controller
kubectl.sh create -f $(dirname $0)/ingestion-api-ctrl.yaml

sleep 5

# add services connections
kubectl.sh create -f $(dirname $0)/ingestion-api-service.yaml
