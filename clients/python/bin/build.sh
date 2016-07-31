#!/bin/bash

export BASE=$(dirname $0)/..

mkdir -p $BASE/info
cp $BASE/../../docs/clients/python-client.md $BASE/info
cp $BASE/../../samsara.version $BASE/info

python setup.py install
