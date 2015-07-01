#!/bin/bash

kubectl.sh delete -f $(dirname $0)/riemann-service.yaml
kubectl.sh delete -f $(dirname $0)/grafana-service.yaml

# now remove controller
kubectl.sh resize rc monitoring --replicas=0
sleep 10
kubectl.sh delete -f $(dirname $0)/monitoring-ctrl.yaml
