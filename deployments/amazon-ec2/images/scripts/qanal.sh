#!/bin/bash -e
#
# $1 - OPTIONAL
#      The name of the docker image to use.
#      ex:
#        mytest/myimage:1.0
#        some.private.docker.repo:5000/mytest/myimage:1.0
#


if [ "$(id -u)" != "0" ] ; then
    echo "Running the script with root privilege's"
    sudo "$0" "$*"
    exit $?
fi

echo "waiting for system to fully come online."
sleep 30


IMAGE=${1:-samsara/qanal}
echo '------------------------------------------------------------------'
echo '                    Using image:' $IMAGE
echo '------------------------------------------------------------------'
mkdir -p /etc/samsara/images
echo "$IMAGE" > /etc/samsara/images/qanal


echo '------------------------------------------------------------------'
echo '                    Setup upstart service'
echo '------------------------------------------------------------------'
cat >/etc/init/qanal.conf <<\EOF
description "Samsara Qanal container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm qanal | true
exec /usr/bin/docker run --name qanal \
       --dns $(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       -p 15000:15000 \
       -v /logs/qanal:/logs \
       -e KAFKA_PORT_9092_TCP_ADDR=kafka.service.consul \
       -e ZOOKEEPER_PORT_2181_TCP_ADDR=zookeeper.service.consul \
       -e ZOOKEEPER_PORT_2181_TCP=tcp://zookeeper.service.consul:2181 \
       -e RIEMANN_PORT_5555_TCP_ADDR=riemann.service.consul \
       -e ELS_PORT_9200_TCP_ADDR=els.service.consul \
       -e 'KAFKA_TOPICS_SPEC={ :topic "events" :partitions :all :type :plain :indexing {:strategy :daily :base-index "events" :doc-type "events" :timestamp-field "timestamp" :timestamp-field-format :millis :id-field "id"}}' \
       -e "TRACKING_ENABLED=true" \
       `cat /etc/samsara/images/qanal`

pre-stop script
        /usr/bin/docker stop qanal
        /usr/bin/docker rm qanal
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull `cat /etc/samsara/images/qanal`


echo '------------------------------------------------------------------'
echo '                add service to consul'
echo '------------------------------------------------------------------'
cat > /etc/consul.d/qanal.json <<\EOF
{
  "service": {
    "name": "qanal",
    "tags": []
  }
}
EOF
