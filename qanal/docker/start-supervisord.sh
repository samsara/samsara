#!/bin/bash

# background daemon
/usr/bin/supervisord -c /etc/supervisor/supervisord.conf

# if you attach the container you get access to supervisorctl
while true ; do
    /usr/bin/supervisorctl
    sleep 3
done