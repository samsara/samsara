#!/bin/bash -e
#
#

if [ "$1" == "1" ] ; then
    echo "(*) Installing private docker registry: $2"
    echo "DOCKER_OPTS=\"\$DOCKER_OPTS --insecure-registry $2\"" | sudo tee -a /etc/default/docker
    sudo service docker restart
fi

echo '--------------------| /etc/default/docker |--------------------'
cat /etc/default/docker || true
echo '---------------------------------------------------------------'
