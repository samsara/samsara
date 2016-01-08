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


IMAGE=${1:-samsara/monitoring}
echo '------------------------------------------------------------------'
echo '                    Using image:' $IMAGE
echo '------------------------------------------------------------------'
mkdir -p /etc/samsara/images
echo "$IMAGE" > /etc/samsara/images/monitoring


echo '------------------------------------------------------------------'
echo '                    Setup upstart service'
echo '------------------------------------------------------------------'
cat >/etc/init/monitoring.conf <<\EOF
description "Samsara Monitor container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm monitoring | true
exec /usr/bin/docker run --name monitoring \
       --dns $(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       -p 5555:5555 \
       -p 5556:5556 \
       -p 8083:8083 \
       -p 8086:8086 \
       -p 15000:80 \
       -v /logs/monitoring:/logs \
       -v /data/monitoring:/data \
       `cat /etc/samsara/images/monitoring`

pre-stop script
        /usr/bin/docker stop monitoring
        /usr/bin/docker rm monitoring
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull `cat /etc/samsara/images/monitoring`


echo '------------------------------------------------------------------'
echo '                add service to consul'
echo '------------------------------------------------------------------'
cat > /etc/consul.d/monitoring.json <<\EOF
{
  "services": [
    {
      "name": "riemann",
      "tags": ["monitoring"],
      "port": 5555
    }, {
      "name": "graphana",
      "tags": ["monitoring"],
      "port": 15000
    }
  ],
  "checks": [
   {
    "id": "riemann-port",
    "name": "Riemann client port open check",
    "script": "/bin/nc -vz -w 1 $(curl -s http://169.254.169.254/latest/meta-data/local-ipv4) 5555",
    "interval": "5s"
   }, {
    "id": "graphana-port",
    "name": "graphana client port http check",
    "script": "/bin/nc -vz -w 1 $(curl -s http://169.254.169.254/latest/meta-data/local-ipv4) 15000",
    "interval": "5s"
   }]
}
EOF
