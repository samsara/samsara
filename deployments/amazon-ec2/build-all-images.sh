#!/bin/bash -e

export LOGDIR=/tmp/samsara-builds


function banner-msg(){
    echo '-----------------------------------------------------------------'
    printf "%25s\n" "$1"
    echo '-----------------------------------------------------------------'
}

function build-image(){
    banner-msg "Building: $1"
    packer build -color=false $1 2>&1 | tee $LOGDIR/$(basename $1).log
}


function get-image-ami() {
    grep -A 1 "Creating the AMI" $LOGDIR/$1.log | tail -1| sed 's/.* ami-/ami-/g'
}

function build-image-with(){
    echo "Using previous image from: $1"
    AMI=$(get-image-ami $1)
    echo "      AMI: $AMI"
    SPEC=/tmp/$(basename $2)
    cat $2 | sed "s/\"source_ami\": \"[^\"]*\",/\"source_ami\": \"$AMI\",/g" > $SPEC
    build-image $SPEC
}


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
build-image      packer-base-image.json
build-image-with packer-base-image.json packer-base-image-with-storage.json
