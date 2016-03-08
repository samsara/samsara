#!/bin/bash

export BASE=$(cd $(dirname $0)/.. && pwd)
export DATE=$(date +%F)
export VER=$(cat $BASE/../../../samsara.version | sed 's/-SNAPSHOT//g')

rm -fr $BASE/tmp
mkdir -p $BASE/tmp

# execute test bench with 1 event as payload
export SCRIPT=perf-001-events
echo '---------------------------------------------'
echo '           running:' $SCRIPT
echo '---------------------------------------------'
$BASE/bin/run-perf-script.sh $BASE/scripts/$SCRIPT.lua &> $BASE/tmp/${DATE}-v${VER}-${SCRIPT}-output.txt-1
$BASE/bin/run-perf-script.sh $BASE/scripts/$SCRIPT.lua &> $BASE/tmp/${DATE}-v${VER}-${SCRIPT}-output.txt-2
$BASE/bin/run-perf-script.sh $BASE/scripts/$SCRIPT.lua &> $BASE/tmp/${DATE}-v${VER}-${SCRIPT}-output.txt-3

echo '---------------------------------------------'
echo '               cooling down'
echo '---------------------------------------------'
sleep 20

# execute test bench with 100 events as payload
export SCRIPT=perf-100-events
echo '---------------------------------------------'
echo '           running:' $SCRIPT
echo '---------------------------------------------'
$BASE/bin/run-perf-script.sh $BASE/scripts/$SCRIPT.lua &> $BASE/tmp/${DATE}-v${VER}-${SCRIPT}-output.txt-1
$BASE/bin/run-perf-script.sh $BASE/scripts/$SCRIPT.lua &> $BASE/tmp/${DATE}-v${VER}-${SCRIPT}-output.txt-2
$BASE/bin/run-perf-script.sh $BASE/scripts/$SCRIPT.lua &> $BASE/tmp/${DATE}-v${VER}-${SCRIPT}-output.txt-3
