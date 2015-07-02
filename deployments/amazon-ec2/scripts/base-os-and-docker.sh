#!/bin/bash -e

if [ "$(id -u)" != "0" ] ; then
    echo "Running the script with root privilege's"
    sudo "$0"
    exit $?
fi

echo "waiting for system to fully come online."
sleep 30


echo '------------------------------------------------------------------'
echo '                    OS update and tools installation'
echo '------------------------------------------------------------------'
apt-get update
apt-get upgrade -y
apt-get install -y wget curl byobu htop bmon iftop sysstat netcat-openbsd
echo 'ENABLED="true"' | tee /etc/default/sysstat


echo '------------------------------------------------------------------'
echo '                         Docker installation'
echo '------------------------------------------------------------------'
apt-get install -y linux-image-extra-`uname -r` linux-image-extra-virtual
wget -qO- https://get.docker.com/ | sh
usermod -aG docker ubuntu
wget https://github.com/docker/compose/releases/download/1.3.1/docker-compose-`uname -s`-`uname -m` -O /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose


echo '------------------------------------------------------------------'
echo '                      max open files configuration'
echo '------------------------------------------------------------------'

sysctl -w fs.file-max=200000
echo 'fs.file-max = 200000' | tee -a /etc/sysctl.conf
sysctl -p
sysctl fs.file-max
cat <<EOF | tee -a /etc/security/limits.conf
* soft nofile 200000
* hard nofile 200000
EOF
