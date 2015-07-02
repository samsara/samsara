#!/bin/bash -e

if [ "$(id -u)" != "0" ] ; then
    echo "Running the script with root privilege's"
    sudo "$0"
    exit $?
fi


echo '------------------------------------------------------------------'
echo '                    Samsara Zookeeper'
echo '------------------------------------------------------------------'
docker pull samsara/zookeeper

docker run -d --restart=on-failure:10 \
       -p 2181:2181 \
       -p 2888:2888 \
       -p 3888:3888 \
       -p 15000:15000 \
       -v /logs/zk:/logs \
       -v /data/zk:/data \
       -e "ZK_SERVER_ID=1" \
       -e "ADV_IP=$(curl 'http://169.254.169.254/latest/meta-data/local-ipv4')" \
       samsara/zookeeper
