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


IMAGE=${1:-samsara/spark-worker}
echo '------------------------------------------------------------------'
echo '                    Using image:' $IMAGE
echo '------------------------------------------------------------------'
mkdir -p /etc/samsara/images
echo "$IMAGE" > /etc/samsara/images/spark-worker


echo '------------------------------------------------------------------'
echo '                    Setup upstart service'
echo '------------------------------------------------------------------'
cat >/etc/init/spark-worker.conf <<\EOF
description "Samsara Spark-worker container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm spark-worker | true
script

     # wait for at least 2 master ips to come up
     while [ "$(dig +short spark-master.service.consul | wc -l)" -lt "2" ] ; do
       echo "Waiting for more spark-master.service.consul to start up... (found:$(dig +short spark-master.service.consul | wc -l))"
       sleep 3
     done

     export SPARK_MASTERS=`dig +short spark-master.service.consul | sed 's/$/:7077/g' | paste -s -d ','`

     exec /usr/bin/docker run --name spark-worker \
       --net=host \
       -v /logs/spark-worker:/logs \
       -e SPARK_MASTERS=$SPARK_MASTERS \
       -e ADV_IP=$(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       `cat /etc/samsara/images/spark-worker`

end script

pre-stop script
        /usr/bin/docker stop spark-worker
        /usr/bin/docker rm spark-worker
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull `cat /etc/samsara/images/spark-worker`


echo '------------------------------------------------------------------'
echo '                add service to consul'
echo '------------------------------------------------------------------'
cat > /etc/consul.d/spark-worker.json <<\EOF
{
  "service": {
    "name": "spark-worker",
    "tags": [],
    "port": 7078
  },
  "check": {
    "id": "spark-worker-port",
    "name": "Spark Worker port",
    "script": "/bin/nc -vz -w 1 127.0.0.1 7078",
    "interval": "5s"
  }
}
EOF
