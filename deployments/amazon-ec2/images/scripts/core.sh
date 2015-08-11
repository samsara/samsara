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
cat >/etc/init/core.conf <<\EOF
description "Samsara CORE container"
author "Bruno"
start on runlevel [2345]
stop on runlevel [016]
respawn
pre-start exec /usr/bin/docker rm core | true
exec /usr/bin/docker run --name core \
       -dns $(curl "http://169.254.169.254/latest/meta-data/local-ipv4") \
       -p 9000:9000 \
       -p 15000:15000 \
       -v /logs/core:/logs \
       `curl "http://169.254.169.254/latest/user-data"` \
       -e "INDEX_STRATEGY=:daily" \
       -e "TRACKING_ENABLED=true" \
       samsara/samsara-core

pre-stop script
        /usr/bin/docker stop core
        /usr/bin/docker rm core
end script
EOF

echo '------------------------------------------------------------------'
echo '                Pull the latest image'
echo '------------------------------------------------------------------'
docker pull samsara/samsara-core
