#!/bin/bash -e

if [ "$(id -u)" != "0" ] ; then
    echo "Running the script with root's privileges"
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
apt-get install -y wget curl byobu htop bmon iftop sysstat netcat-openbsd telnet
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



echo '------------------------------------------------------------------'
echo '                      logging volume'
echo '------------------------------------------------------------------'
groupadd logger
mkdir -p /logs
chown -R root:logger /logs
chmod -R g+w /logs


echo '------------------------------------------------------------------'
echo '                    installing helper scripts'
echo '------------------------------------------------------------------'
cat >/usr/local/bin/user-data <<\EOF
#!/bin/bash
#
# Helper utility for AWS instance user-data
#
# AUTHOR
#     Bruno Bonacci
#
# DESCRIPTION
#
# When the instance user data is a list of key=value pairs seprated by newline
# with this utility you can fetch individual values by their key.
# It supports also list of values in CSV format.
#
# SYNOPSYS:
#       user-data [key [index]]
#
# WHERE:
#       key - is the name of the key to fetch, if omitted show all
#       index - in case the value is a comma-separated list of items
#               it returns the value specified by the index (starting from 1)
#               If omitted return all of them as CSV.
#

export UD=$(curl -s http://169.254.169.254/latest/user-data)

if [ "$1" != "" ] ; then
  export GUD=$(echo "$UD" | grep $1 | cut -d= -f2)
  if [ "$2" != "" ] ; then
    echo "$GUD" | cut -d, -f $2
  else
    echo "$GUD"
  fi
else
  echo "$UD"
fi
EOF

chmod +x /usr/local/bin/user-data
