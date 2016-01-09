#!/bin/bash

export CUR=$(pwd)
export BASE=`cd $(dirname $0)/.. && pwd`

function banner(){
    echo '--------------------------------------------------------------------------------'
    echo '                              BUILDING :' $1
    echo '--------------------------------------------------------------------------------'
}


## build utils
banner utils
cd $BASE/utils
lein do clean, check, jar, install


## build clients/clojure
banner Clojure client
cd $BASE/clients/clojure
lein do clean, check, jar, install

## build clients/logger
## banner Logger client
## cd $BASE/clients/logger
## lein do clean, check, jar, install


## build ingestion-api
banner ingestion-api
cd $BASE/ingestion-api
lein do clean, check, bin


## build moebius
banner moebius
cd $BASE/moebius
lein do clean, check, jar, install


## build modules
banner modules
cd $BASE/modules
lein do clean, check, jar, install


## build qanal
banner qanal
cd $BASE/qanal
lein do clean, check, bin


# restoring initial dir
cd $CUR
