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
cat >/etc/init/ingestion.conf <<\EOF
description "Ingestion-API container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm ingestion | true
exec /usr/bin/docker run --name ingestion \
       -dns $(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       -p 9000:9000 \
       -p 15000:15000 \
       -v /logs/ingestion:/logs \
       `curl "http://169.254.169.254/latest/user-data"` \
       -e "TRACKING_ENABLED=true" \
       samsara/ingestion-api

pre-stop script
        /usr/bin/docker stop ingestion
        /usr/bin/docker rm ingestion
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull samsara/ingestion-api
