#!/bin/bash

export BASE=$(dirname $0)/..
[ `uname` == "Darwin" ] && export SED=gsed || export SED=sed

export VER=$($SED 's/-.*//g' $BASE/../../samsara.version)
$SED -i "s/version=.*/version='$VER',/g" setup.py

python setup.py install
