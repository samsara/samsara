#!/bin/bash

export BASE=$(dirname $0)

# starting zookeeper and kafka
$BASE/zookeeper/start.sh

# starting ingestion api
$BASE/ingestion-api/start.sh
