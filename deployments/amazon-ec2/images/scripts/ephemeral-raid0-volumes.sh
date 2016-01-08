#!/bin/bash -e

if [ "$(id -u)" != "0" ] ; then
    echo "Running the script with root's privileges"
    sudo "$0"
    exit $?
fi

echo "waiting for system to fully come online."
sleep 30

echo '------------------------------------------------------------------'
echo '          ephemeral  /data and /logs volumes installation'
echo '------------------------------------------------------------------'
cat > /etc/init.d/ephemeral-raid0 <<\EOF
#!/bin/bash
### BEGIN INIT INFO
# Provides:          ephemeral-raid0
# Required-Start:    $syslog
# Required-Stop:     $syslog
# Should-Start:
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: RAID0 on ephemeral storage
# Description:       RAID0 on ephemeral storage
### END INIT INFO

VG_NAME=ephemeral

. /lib/lsb/init-functions

ephemeral_start() {

    RAID_OK=`/sbin/mdadm --detail --scan`

    if [ "$RAID_OK" == "" ] ; then

        METADATA_URL_BASE="http://169.254.169.254/2012-01-12"

        # Configure Raid - take into account xvdb or sdb
        root_drive=`df -h | grep -v grep | awk 'NR==2{print $1}'`

        if [ "$root_drive" == "/dev/xvda1" ]; then
            echo "Detected 'xvd' drive naming scheme (root: $root_drive)"
            DRIVE_SCHEME='xvd'
        else
            echo "Detected 'sd' drive naming scheme (root: $root_drive)"
            DRIVE_SCHEME='sd'
        fi

        # figure out how many ephemerals we have by querying the metadata API, and then:
        #  - convert the drive name returned from the API to the hosts DRIVE_SCHEME, if necessary
        #  - verify a matching device is available in /dev/
        drives=""
        ephemeral_count=0
        ephemerals=$(/usr/bin/curl --silent $METADATA_URL_BASE/meta-data/block-device-mapping/ | grep ephemeral)
        for e in $ephemerals; do
          echo "Probing $e .."
          device_name=$(/usr/bin/curl --silent $METADATA_URL_BASE/meta-data/block-device-mapping/$e)
          # might have to convert 'sdb' -> 'xvdb'
          device_name=$(echo $device_name | sed "s/sd/$DRIVE_SCHEME/")
          device_path="/dev/$device_name"

          # test that the device actually exists since you can request more ephemeral drives than are available
          # for an instance type and the meta-data API will happily tell you it exists when it really does not.
          if [ -b $device_path ]; then
            echo "Detected ephemeral disk: $device_path"
            drives="$drives $device_path"
            ephemeral_count=$((ephemeral_count + 1 ))
          else
            echo "Ephemeral disk $e, $device_path is not present. skipping"
          fi
        done
        if [ "$ephemeral_count" = 0 ]; then
          echo "No ephemeral disk detected. exiting"
          exit 0
        fi

        # ephemeral0 is typically mounted for us already. umount it here
        /bin/umount /mnt

        # overwrite first few blocks in case there is a filesystem, otherwise mdadm will prompt for input
        for drive in $drives; do
          dd if=/dev/zero of=$drive bs=4096 count=1024
        done

        # ignore possible error here
        # Error: /dev/xvdb: unrecognised disk label
        /sbin/partprobe

        /sbin/mdadm --create --verbose /dev/md0 --level=0 -c256 --name=RAID0_VOL --raid-devices=$ephemeral_count $drives
        UUID=$(/sbin/mdadm --detail --scan | grep -oP 'UUID=([^ ]*)')
        echo DEVICE $drives | tee /etc/mdadm/mdadm.conf
        echo "ARRAY /dev/md0 $UUID" >> /etc/mdadm/mdadm.conf
        /sbin/blockdev --setra 65536 /dev/md0
        /sbin/mkfs -t ext4 -L RAID0_VOL /dev/md0

        # Remove xvdb/sdb from fstab
        sed -i "/${DRIVE_SCHEME}b/d" /etc/fstab

    fi



    # do mnt
    /bin/mount -t ext4 -o noatime LABEL=RAID0_VOL /mnt
    /bin/chmod 755 /mnt

    # set up /data and /logs
    /bin/mkdir -p /mnt/data /mnt/logs
    /bin/rm -fr /data /logs
    /bin/ln -fs /mnt/data /data
    /bin/ln -fs /mnt/logs /logs
    chown -R root:logger /mnt /mnt/logs
    chmod -R g+w /mnt/logs

    log_end_msg 0
} # ephemeral_start

ephemeral_stop() {
    /bin/umount /mnt

    /sbin/vgchange -an "$VG_NAME"

    log_end_msg 0
} # ephemeral_stop


case "$1" in
  start)
        log_daemon_msg "Mounting ephemeral volumes" "ephemeral-raid0"
        ephemeral_start
        ;;

  stop)
        log_daemon_msg "Umounting ephemeral volumes" "ephemeral-raid0"
        ephemeral_stop
        ;;

  *)
        echo "Usage: /etc/init.d/ephemeral-raid0 {start|stop}"
        exit 1
esac

exit 0
EOF

DEBIAN_FRONTEND=noninteractive apt-get -qyy install mdadm curl
chown root:root /etc/init.d/ephemeral-raid0
chmod 755 /etc/init.d/ephemeral-raid0
update-rc.d ephemeral-raid0 defaults 00
