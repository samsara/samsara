#!/bin/bash

export VERSION=0.1.0

cat <<EOF
-----------------------------------------------
   _____
  / ___/____ _____ ___  _________ __________ _
  \__ \/ __ `/ __ `__ \/ ___/ __ `/ ___/ __ `/
 ___/ / /_/ / / / / / (__  ) /_/ / /  / /_/ /
/____/\__,_/_/ /_/ /_/____/\__,_/_/   \__,_/

  Kubernetes set up for Samsara Analytics
-----------------------------| $VERSION  |-------

EOF

export BASE=$(dirname $0)

# starting zookeeper and kafka
$BASE/zookeeper2/start.sh
$BASE/kafka/start.sh

# starting ingestion api
$BASE/ingestion-api/start.sh
