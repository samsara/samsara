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
build-image-with $(get-image-ami packer-base-image.json) packer-ingestion-api-image.json $*
build-image-with $(get-image-ami packer-base-image.json) packer-kibana-image.json $*
build-image-with $(get-image-ami packer-base-image-with-storage.json) packer-els-image.json $*
build-image-with $(get-image-ami packer-base-image.json) packer-qanal-image.json $*
build-image-with $(get-image-ami packer-base-image.json) packer-core-image.json $*


#
# OUTPUT AMI
#
cat <<EOF


                  # PUT this in your terraform.tfvars
----------8<----------8<----------8<----------8<----------8<----------
base_ami = "$(get-image-ami packer-base-image.json)"
data_ami = "$(get-image-ami packer-base-image-with-storage.json)"
ingestion_ami = "$(get-image-ami packer-ingestion-api-image.json)"
els_ami = "$(get-image-ami packer-els-image.json)"
kibana_ami = "$(get-image-ami packer-kibana-image.json)"
qanal_ami = "$(get-image-ami packer-qanal-image.json)"
core_ami = "$(get-image-ami packer-core-image.json)"
----------8<----------8<----------8<----------8<----------8<----------
EOF
