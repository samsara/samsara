#!/bin/bash -e
#
# $1 - OPTIONAL
#      The name of the docker image to use.
#      ex:
#        mytest/myimage:1.0
#        some.private.docker.repo:5000/mytest/myimage:1.0
#


if [ "$(id -u)" != "0" ] ; then
    echo "Running the script with root's privileges"
    sudo "$0" "$*"
    exit $?
fi

echo "waiting for system to fully come online."
sleep 30


IMAGE=${1:-samsara/elasticsearch}
echo '------------------------------------------------------------------'
echo '                    Using image:' $IMAGE
echo '------------------------------------------------------------------'
mkdir -p /etc/samsara/images
echo "$IMAGE" > /etc/samsara/images/els


echo '------------------------------------------------------------------'
echo '                    Setup upstart service'
echo '------------------------------------------------------------------'
cat >/etc/init/elasticsearch.conf <<\EOF
description "ElasticSearch container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm els | true
exec /usr/bin/docker run --name els \
       --dns $(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       -p 9200:9200 \
       -p 9300:9300 \
       -p 15000:15000 \
       -v /logs/els:/logs \
       -v /data/els:/data \
       -e "ADV_IP=$(curl 'http://169.254.169.254/latest/meta-data/local-ipv4')" \
       -e ZOOKEEPER_PORT_2181_TCP_ADDR=zookeeper.service.consul \
       -e "ZOOKEEPER_PORT_2181_TCP_PORT=2181" \
       -e ELS_NODE_TYPE="$(user-data ELS_NODE_TYPE)" \
       -e AWS_ACCESS_KEY="$(user-data AWS_ACCESS_KEY)" \
       -e AWS_SECRET_KEY="$(user-data AWS_SECRET_KEY)" \
       -e AWS_REGION="$(user-data AWS_REGION)" \
       -e AWS_REPOS_ACCESS_KEY="$(user-data AWS_REPOS_ACCESS_KEY)" \
       -e AWS_REPOS_SECRET_KEY="$(user-data AWS_REPOS_SECRET_KEY)" \
       -e AWS_REPOS_NAME="$(user-data AWS_REPOS_NAME)" \
       -e AWS_REPOS_BASE_PATH="$(user-data AWS_REPOS_BASE_PATH)" \
       `cat /etc/samsara/images/els`

pre-stop script
        /usr/bin/docker stop els
        /usr/bin/docker rm els
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull `cat /etc/samsara/images/els`


echo '------------------------------------------------------------------'
echo '                add service to consul'
echo '------------------------------------------------------------------'
cat > /etc/consul.d/els.json <<\EOF
{
  "service": {
    "name": "els",
    "tags": [],
    "port": 9200
  },
  "check": {
    "id": "els-port",
    "name": "ELS client port open check",
    "script": "curl -is -m 1 http://$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4):9200/",
    "interval": "5s"
  }
}
EOF
