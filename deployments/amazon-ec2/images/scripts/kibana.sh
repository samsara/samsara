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
cat >/etc/init/kibana.conf <<\EOF
description "Kibana container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm kibana | true
exec /usr/bin/docker run --name kibana \
       -dns $(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       -p 8000:8000 \
       -p 15000:15000 \
       -v /logs/kibana:/logs \
       `curl "http://169.254.169.254/latest/user-data"` \
       samsara/kibana

pre-stop script
        /usr/bin/docker stop kibana
        /usr/bin/docker rm kibana
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull samsara/kibana
