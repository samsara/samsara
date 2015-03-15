#!/bin/bash

# background daemon
/usr/bin/supervisord -c /etc/supervisor/supervisord.conf

while true ; do
    /usr/bin/supervisorctl
    sleep 3
done
