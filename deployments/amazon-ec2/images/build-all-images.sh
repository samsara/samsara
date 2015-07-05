#!/bin/bash -e

export LOGDIR=/tmp/samsara-builds


function banner-msg(){
    echo '-----------------------------------------------------------------'
    printf "%25s\n" "$1"
    echo '-----------------------------------------------------------------'
}


function build-image(){
    banner-msg "Building: $1"
    IMG=$1
    shift
    packer build -color=false $* -var "build_id=${BASE_NAME}" $IMG 2>&1 | tee $LOGDIR/$(basename $IMG).log
}


function get-image-ami() {
    grep -A 1 "Creating the AMI" $LOGDIR/$1.log | tail -1| sed 's/.* ami-/ami-/g'
}

function build-image-with(){
    AMI=$1
    IMG=$2
    echo "Using AMI: $AMI"
    shift 2
    build-image $IMG -var "source_ami=$AMI" $*
}


#
# Getting BASE_NAME
#

if [ "$1" == "" ] ; then
    echo "Please provide base name:"
    echo "     $0 samsara-v01"
    exit 1
fi
export BASE_NAME=$1
shift


#
# Checking packer installation
#

if [ "$(which packer)" = "" ] ; then
    echo 'You need to install packer.io in order to build Samsara images.'
    echo 'please go to https://packer.io/intro/getting-started/setup.html'
    echo 'for installation instructions.'
    exit 1
fi


#
# Validate configurations
#
find . -name packer-\*.json -type f | xargs -I {} packer validate {}


#
# Cleanup logs
#
rm -fr $LOGDIR
mkdir $LOGDIR


#
# Building images
#
build-image      packer-base-image.json $*
build-image-with $(get-image-ami packer-base-image.json) packer-base-image-with-storage.json $*


#
# OUTPUT AMI
#
echo "BASE ami id:" $(get-image-ami packer-base-image.json)
echo "DATA ami id:" $(get-image-ami packer-base-image-with-storage.json)
