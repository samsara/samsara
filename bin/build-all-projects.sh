#!/bin/bash -e
#
# build-all-projects.sh builds all the sub projects and installs
# the artifacts into the local maven repo
#
# usage:
#    ./build-all-projects.sh
#         - builds all the projects
#    ./build-all-projects.sh -test
#         - builds and run the tests on all the projects


export CUR=$(pwd)
export BASE=`cd $(dirname $0)/.. && pwd`

[ "$1" = "-test" ] && export TEST="check, test" || export TEST="check"

function banner(){
    echo '--------------------------------------------------------------------------------'
    echo '                              BUILDING :' $*
    echo '--------------------------------------------------------------------------------'
}

[ "$1" = "-test" ] && banner Samsara with tests || banner Samsara without tests

# numer of test-check iterations
export TC_NUM_TESTS=${TC_NUM_TESTS:-50}

## build utils
banner utils
cd $BASE/internal/utils
lein do clean, $TEST, jar, install


## build clients/clojure
banner Clojure client
cd $BASE/clients/clojure
lein do clean, $TEST, jar, install


## build clients/logger
banner Logger client
cd $BASE/clients/logger
lein do clean, $TEST, jar, install


## build ingestion-api
banner ingestion-api
cd $BASE/ingestion-api
lein do clean, $TEST, bin


## build moebius
banner moebius
cd $BASE/moebius
lein do clean, $TEST, jar, install


## build modules
banner modules
cd $BASE/modules
lein do clean, $TEST, jar, install


## build core
banner CORE
cd $BASE/core
lein do clean, $TEST, jar, install, bin


## build qanal
banner qanal
cd $BASE/qanal
lein do clean, $TEST, bin


## build qanal-refactor
banner qanal-refactor
cd $BASE/qanal-refactor
lein do clean, compile :all, $TEST


# restoring initial dir
cd $CUR
