#!/bin/bash


# now add controller
kubectl.sh create -f $(dirname $0)/els-ctrl.yaml

# now add controller
kubectl.sh create -f $(dirname $0)/kibana-ctrl.yaml

# now add services
kubectl.sh create -f $(dirname $0)/riemann-service.yaml
kubectl.sh create -f $(dirname $0)/grafana-service.yaml
