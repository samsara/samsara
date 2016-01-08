#!/bin/bash -e

if [ "$(id -u)" != "0" ] ; then
    echo "Running the script with root's privileges"
    sudo "$0"
    exit $?
fi

export CONSUL_VERSION=0.5.2

echo '------------------------------------------------------------------'
echo '                    Consul server installation'
echo '------------------------------------------------------------------'

echo "(*) Consul agent must be already installed..."
which consul

echo "(*) Consul ui installation..."
wget "https://dl.bintray.com/mitchellh/consul/${CONSUL_VERSION}_web_ui.zip" \
     -O /tmp/consul-ui.zip && \
    unzip /tmp/consul-ui.zip -d /tmp && \
    mv /tmp/dist /var/lib/consul-ui && \
    rm -fr /tmp/consul-ui.zip

echo "(*) Consul data dir"
mkdir -p /data/consul
chown -R consul:consul /data/consul

echo "(*) Consul upstart configuration"
cat > /etc/init/consul.conf <<\EOF
description "Consul server"
author "Bruno"
setuid consul
start on runlevel [2345]
stop on runlevel [016]
respawn
script
   exec >> /logs/consul.log 2>&1
   exec /usr/sbin/consul agent -server \
       -bootstrap-expect 3 \
       -retry-join $(user-data CONSUL 1) \
       -retry-join $(user-data CONSUL 2) \
       -retry-join $(user-data CONSUL 3) \
       -data-dir /data/consul \
       -config-dir /etc/consul.d \
       -ui-dir /var/lib/consul-ui
end script
EOF
