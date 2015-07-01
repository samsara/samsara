#!/bin/bash


# now add controller
kubectl.sh resize rc core --replicas=0
sleep 10
kubectl.sh delete -f $(dirname $0)/core-ctrl.yaml
