#!/bin/bash -e

if [ "$(id -u)" != "0" ] ; then
    echo "Running the script with root's privileges"
    sudo "$0"
    exit $?
fi


echo '------------------------------------------------------------------'
echo '                  /data and /logs volumes installation'
echo '------------------------------------------------------------------'
apt-get install -y lvm2
pvcreate /dev/xvdd
vgcreate vg0 /dev/xvdd
lvcreate -l 90%vg -n data vg0
lvcreate -l 10%vg -n logs vg0
mkfs.ext4 /dev/vg0/data
mkfs.ext4 /dev/vg0/logs
mkdir -p -m 000 /data
mkdir -p -m 000 /logs
echo '/dev/vg0/data /data ext4 defaults 0 0' | tee -a /etc/fstab
echo '/dev/vg0/logs /logs ext4 defaults 0 0' | tee -a /etc/fstab
mount /data
mount /logs
chown -R root:logger /logs
chmod -R g+w /logs
