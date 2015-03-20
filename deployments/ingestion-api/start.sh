#!/bin/bash

# add services connections
kubectl.sh create -f $(dirname $0)/ingestion-api-service.yaml

# now add controller
kubectl.sh create -f $(dirname $0)/ingestion-api-ctrl.yaml
