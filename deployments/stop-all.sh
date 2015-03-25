#!/bin/bash

export BASE=$(dirname $0)

# stopping ingestion api
$BASE/ingestion-api/stop.sh

# stopping qanal
$BASE/qanal/stop.sh

# starting zookeeper and kafka
$BASE/kafka/stop.sh
$BASE/zookeeper2/stop.sh


# eliminating resudial pods
kubectl.sh get pods --no-headers=true | cut -d' ' -f1 | xargs -I {} kubectl.sh delete pod {}
