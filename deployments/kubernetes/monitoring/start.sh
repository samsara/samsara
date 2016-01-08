#!/bin/bash


# now add controller
kubectl.sh create -f $(dirname $0)/monitoring-ctrl.yaml

# now add services
kubectl.sh create -f $(dirname $0)/riemann-service.yaml
kubectl.sh create -f $(dirname $0)/grafana-service.yaml
