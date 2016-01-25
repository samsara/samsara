#!/bin/bash

export MBIN=$(dirname $0)
export MYSQL_ARGS='--user=root --datadir=/data/grafana/mysql --general-log-file=/logs/mysql.log --log-error=/logs/mysql-errors.log --lc-messages-dir=/opt/mysql/share/'

if [ ! -d /data/grafana/mysql ] ; then
    echo "Initializing database"
    mkdir -p /data/grafana
    $MBIN/mysqld $MYSQL_ARGS --initialize-insecure
    $MBIN/mysqld_safe $MYSQL_ARGS --port=9999 &
    sleep 3
    $MBIN/mysqladmin --port=9999 -u root password samsara
    $MBIN/mysql --port=9999 -u root -psamsara -e "create database grafana; GRANT ALL PRIVILEGES ON grafana.* TO grafana@localhost IDENTIFIED BY 'grafana'; flush privileges;"
    $MBIN/mysqladmin --port=9999 -uroot -psamsara shutdown
    sleep 3
fi

echo "Starting database"
$MBIN/mysqld $MYSQL_ARGS
