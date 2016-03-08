#!/bin/bash

# cloned and built from wrk2 project (https://github.com/giltene/wrk2)

$(dirname $0)/wrk2 -t5 -c100 -d50s -R2000 --latency -s $1 http://127.0.0.1:9000/v1/events
