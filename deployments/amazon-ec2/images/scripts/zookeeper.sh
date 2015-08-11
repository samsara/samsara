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
cat >/etc/init/zookeeper.conf <<\EOF
description "Zookeeper container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm zookeeper | true
exec /usr/bin/docker run --name zookeeper \
       -dns $(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       -p 2181:2181 \
       -p 2888:2888 \
       -p 3888:3888 \
       -p 15000:15000 \
       -v /logs/zk:/logs \
       -v /data/zk:/data \
       -e "ZK_SERVER_ID=$(user-data ZK_SERVER_ID)" \
       -e "ZOOKEEPER1_PORT_2181_TCP=tcp://$(user-data ZOOKEEPER 1)" \
       -e "ZOOKEEPER2_PORT_2181_TCP=tcp://$(user-data ZOOKEEPER 2)" \
       -e "ZOOKEEPER3_PORT_2181_TCP=tcp://$(user-data ZOOKEEPER 3)" \
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


echo '------------------------------------------------------------------'
echo '                add service to consul'
echo '------------------------------------------------------------------'
cat > /etc/consul.d/zookeeper.json <<\EOF
{
  "service": {
    "name": "zookeeper",
    "tags": ["zk"],
    "port": 2181
  },
  "check": {
    "id": "RUOK",
    "name": "Zookeeper sanity check",
    "script": "/var/lib/consul-check/zookeeper-check.sh",
    "interval": "5s"
  }
}
EOF

cat > /var/lib/consul-check/zookeeper-check.sh <<\EOF
#!/bin/bash -ex
[ "imok" = "$(echo ruok | nc -w1 $(curl -s http://169.254.169.254/latest/meta-data/local-ipv4) 2181)" ]
EOF

chmod +x /var/lib/consul-check/zookeeper-check.sh
