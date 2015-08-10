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
# TODO: zookeeper clique must be retrieved as userdata
cat >/etc/init/zookeeper.conf <<\EOF
description "Zookeeper container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm zookeeper | true
exec /usr/bin/docker run --name zookeeper \
       -p 2181:2181 \
       -p 2888:2888 \
       -p 3888:3888 \
       -p 15000:15000 \
       -v /logs/zk:/logs \
       -v /data/zk:/data \
       -e "ZK_SERVER_ID=$(curl -s http://169.254.169.254/latest/user-data | grep ZK_SERVER_ID | cut -d= -f2)" \
       -e "ZOOKEEPER1_PORT_2181_TCP=tcp://10.10.1.5:2181" \
       -e "ZOOKEEPER2_PORT_2181_TCP=tcp://10.10.2.5:2181" \
       -e "ZOOKEEPER3_PORT_2181_TCP=tcp://10.10.3.5:2181" \
       samsara/zookeeper

pre-stop script
        /usr/bin/docker stop zookeeper
        /usr/bin/docker rm zookeeper
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull samsara/zookeeper
