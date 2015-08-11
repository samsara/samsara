#!/bin/bash -e

if [ "$(id -u)" != "0" ] ; then
    echo "Running the script with root privilege's"
    sudo "$0"
    exit $?
fi

echo "waiting for system to fully come online."
sleep 30


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
       -dns $(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       -p 15000:15000 \
       -v /logs/qanal:/logs \
       -e KAFKA_PORT_9092_TCP_ADDR=kafka.service.consul \
       -e ZOOKEEPER_PORT_2181_TCP_ADDR=zookeeper.service.consul \
       -e ZOOKEEPER_PORT_2181_TCP=tcp://zookeeper.service.consul:2181 \
       -e RIEMANN_PORT_5555_TCP_ADDR=riemann.service.consul \
       -e ELS_PORT_9200_TCP_ADDR=els.service.consul \
       -e "KAFKA_TOPIC=events" \
       -e "KAFKA_PARTITIONS=:all" \
       -e "TRACKING_ENABLED=true" \
       samsara/qanal

pre-stop script
        /usr/bin/docker stop qanal
        /usr/bin/docker rm qanal
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull samsara/qanal


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
