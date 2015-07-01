#!/bin/bash


# now add controller
kubectl.sh create -f $(dirname $0)/core-ctrl.yaml
