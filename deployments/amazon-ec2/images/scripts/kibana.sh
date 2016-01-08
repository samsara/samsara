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


IMAGE=${1:-samsara/kibana}
echo '------------------------------------------------------------------'
echo '                    Using image:' $IMAGE
echo '------------------------------------------------------------------'
mkdir -p /etc/samsara/images
echo "$IMAGE" > /etc/samsara/images/kibana


echo '------------------------------------------------------------------'
echo '                    Setup upstart service'
echo '------------------------------------------------------------------'
cat >/etc/init/kibana.conf <<\EOF
description "Kibana container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm kibana | true
exec /usr/bin/docker run --name kibana \
       --dns $(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       -p 8000:8000 \
       -p 15000:15000 \
       -v /logs/kibana:/logs \
       -e ELASTICSEARCH_PORT_9200_TCP_ADDR=$(user-data ELS) \
       -e ELASTICSEARCH_PORT_9200_TCP_PORT=9200 \
       `cat /etc/samsara/images/kibana`

pre-stop script
        /usr/bin/docker stop kibana
        /usr/bin/docker rm kibana
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull `cat /etc/samsara/images/kibana`


echo '------------------------------------------------------------------'
echo '                add service to consul'
echo '------------------------------------------------------------------'
cat > /etc/consul.d/kibana.json <<\EOF
{
  "service": {
    "name": "kibana",
    "tags": [],
    "port": 8000
  },
  "check": {
    "id": "kibana-http-status",
    "name": "Kibana client port check",
    "script": "curl -is -m 1 http://$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4):8000/",
    "interval": "5s"
  }
}
EOF
