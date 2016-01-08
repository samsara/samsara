#!/bin/bash


# now add controller
kubectl.sh create -f $(dirname $0)/qanal-ctrl.yaml
