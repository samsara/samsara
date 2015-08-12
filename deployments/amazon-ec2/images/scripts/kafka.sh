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
cat >/etc/init/kafka.conf <<\EOF
description "Kafka container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm kafka | true
exec /usr/bin/docker run --name kafka \
       --dns $(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       -p 9092:9092 \
       -p 15000:15000 \
       -v /logs/kafka:/logs \
       -v /data/kafka:/data \
       -e "KAFKA_BROKER_ID=$(user-data KAFKA_BROKER_ID)" \
       -e "ADV_IP=$(curl 'http://169.254.169.254/latest/meta-data/local-ipv4')" \
       -e "ZOOKEEPER_PORT_2181_TCP_ADDR=zookeeper.service.consul" \
       samsara/kafka

pre-stop script
        /usr/bin/docker stop kafka
        /usr/bin/docker rm kafka
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull samsara/kafka


echo '------------------------------------------------------------------'
echo '                add service to consul'
echo '------------------------------------------------------------------'
cat > /etc/consul.d/kafka.json <<\EOF
{
  "service": {
    "name": "kafka",
    "tags": [],
    "port": 9092
  },
  "check": {
    "id": "kafka-port",
    "name": "Kafka client port open check",
    "script": "/bin/nc -vz -w 1 $(curl -s http://169.254.169.254/latest/meta-data/local-ipv4) 9092",
    "interval": "5s"
  }
}
EOF
