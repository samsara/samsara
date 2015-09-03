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


IMAGE=${1:-samsara/samsara-core}
echo '------------------------------------------------------------------'
echo '                    Using image:' $IMAGE
echo '------------------------------------------------------------------'
mkdir -p /etc/samsara/images
echo "$IMAGE" > /etc/samsara/images/core


echo '------------------------------------------------------------------'
echo '                    Setup upstart service'
echo '------------------------------------------------------------------'
cat >/etc/init/core.conf <<\EOF
description "Samsara CORE container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm core | true
exec /usr/bin/docker run --name core \
       --dns $(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       -p 4555:4555 \
       -p 15000:15000 \
       -v /logs/core:/logs \
       -e KAFKA_PORT_9092_TCP_ADDR=kafka.service.consul \
       -e KAFKA_PORT_9092_TCP_PORT=9092 \
       -e ZOOKEEPER_PORT_2181_TCP_ADDR=zookeeper.service.consul \
       -e ZOOKEEPER_PORT_2181_TCP_PORT=2181 \
       -e RIEMANN_PORT_5555_TCP_ADDR=riemann.service.consul \
       -e "TRACKING_ENABLED=true" \
       `cat /etc/samsara/images/core`

pre-stop script
        /usr/bin/docker stop core
        /usr/bin/docker rm core
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull `cat /etc/samsara/images/core`


echo '------------------------------------------------------------------'
echo '                add service to consul'
echo '------------------------------------------------------------------'
cat > /etc/consul.d/core.json <<\EOF
{
  "service": {
    "name": "core",
    "tags": [],
    "port": 4555
  },
  "check": {
    "id": "core-nrepl-port",
    "name": "Samsara CORE nREPL port",
    "script": "/bin/nc -vz -w 1 127.0.0.1 4555",
    "interval": "5s"
  }
}
EOF
