#!/bin/bash

kubectl.sh delete -f $(dirname $0)/els-service.yaml
kubectl.sh delete -f $(dirname $0)/kibana-service.yaml

# now remove controller
kubectl.sh resize rc els --replicas=0
sleep 10
kubectl.sh delete -f $(dirname $0)/els-ctrl.yaml


# now remove controller
kubectl.sh resize rc kibana --replicas=0
sleep 10
kubectl.sh delete -f $(dirname $0)/kibana-ctrl.yaml
