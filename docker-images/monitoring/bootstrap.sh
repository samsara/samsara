#!/bin/bash

# log output of this script
exec > >( tee -a /logs/bootstrap.out )
exec 2>&1

# wait for the service to come up
while [ "$(curl -m 1 -s -q -XGET 'http://localhost:8086/ping' >/dev/null || echo 1)" == "1" ] ; do
    echo "Waiting for influxdb to start up..."
    sleep 3
done

echo "database ready"

if [ "$(curl -s -m 1 -XGET 'http://localhost:8086/db?u=root&p=root' | grep -c samsara)" == "0" ]
then
    echo "creating default database"
    curl -s -X POST 'http://localhost:8086/db?u=root&p=root' -d '{"name": "samsara"}'
else
    echo "'samsara' database already present..."
fi

exit 0
