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
/opt/influxdb/usr/bin/influx -execute 'create database if not exists samsara'


# wait for grafana to come up
while [ "$(curl -m 1 -s -q -XGET 'http://admin:admin@localhost/api/org' >/dev/null || echo 1)" == "1" ] ; do
    echo "Waiting for grafana to start up..."
    sleep 3
done

if [ "$(curl -sS "http://$1:$2@localhost/api/datasources" | grep -q samsara && echo 1)" == "1" ] ; then
    echo "datasource already present..."
else
    echo "creating datasource..."
    curl -sS "http://$1:$2@localhost/api/datasources" -H 'Content-Type: application/json' -XPOST -d '{"name":"samsara","type":"influxdb","url":"http://127.0.0.1:8086/","access":"proxy","isDefault":true,"database":"samsara","user":"admin","password":"admin"}'
fi

exit 0
