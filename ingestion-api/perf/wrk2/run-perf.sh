#!/bin/bash

# start the ingestion-api
# then clone and build wrk2 project (https://github.com/giltene/wrk2)
# then set WRK2_HOME directory

$WRK2_HOME/wrk -t5 -c100 -d50s -R2000 --latency -s $1 http://127.0.0.1:9000/v1/events
