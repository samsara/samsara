# samsara-docker-images

Docker images for Samsara's third-party components

## Description

Samsara uses several third-party components, such as Apache ZooKeeper,
Apache Kafka, ElasticSearch, Kibana etc.
The aim of this project is to produce production quality Docker images
for those components.

### How to start a local environment

A local environment is ideal for development and testing purposes.
It setup a fully running cluster with all components but without
fault tolerance.

Before starting make sure that the **data** volume and the **logs** volumes
will be pointing to directories which exists.

To create the default one.
```bash
mkdir -p `grep /tmp fig.yml | cut -d' ' -f6 | cut -d':' -f1 | sort`
```

Additionally if you are running this for the first time, you won't have any
of the docker images. If you want to build them from scratch, then proceed
with the [How To build and push docker images](#how-to-build-and-push-docker-images)
A good suggestion is to start with pulling them all.

```bash
docker pull samsara/ingestion-api
docker pull samsara/qanal

docker pull samsara/zookeeper
docker pull samsara/kafka
docker pull samsara/elasticsearch
docker pull samsara/kibana
docker pull samsara/riemann

docker pull tutum/influxdb
docker pull tutum/grafana
```

Images on the first block are built using their own github project.
Images from the second group are built with this project
Images on the third group are third-party images which should be available
in the Docker Registry.

Now to start the services:

```
fig up -d

# wait for all components to come up

# check the status with
fig ps

```

Once the service is up and running you can then access
the following main services

| service       |               port                               |
|---------------|:------------------------------------------------:|
| ingestion-api | [http://127.0.0.1:9000](http://127.0.0.1:9000)   |
| kibana        | [http://127.0.0.1:8000 ](http://127.0.0.1:8000)  |
| graphana	| [http://127.0.0.1:15000](http://127.0.0.1:15000) |
| elasticsearch | [http://127.0.0.1:9200/_plugin/kopf/](http://127.0.0.1:9200/_plugin/kopf/) |
| elasticsearch | [http://127.0.0.1:9200/_plugin/HQ/  ](http://127.0.0.1:9200/_plugin/HQ/  ) |
| elasticsearch | [http://127.0.0.1:9200/_plugin/head/](http://127.0.0.1:9200/_plugin/head/) |

**NOTE:** if you running on **boot2docker** (Mac OSX) you have to
replace 127.0.0.1 with the ip of the docker vm (typically **192.168.59.103**)

Data paths and logs are mounted on `/tmp/data` and `/tmp/logs` respectively.

**NOTE:** for **boot2docker** these paths will reside in the VM not on the host.


Every container will expose the port `15000` for the `supervisord` admin console.
here a full list of ports

```
               Name                              Command               State                                    Ports
-------------------------------------------------------------------------------------------------------------------------------------------------------
samsaradockerimages_elasticsearch_1   /bin/sh -c /configure-and- ...   Up      0.0.0.0:15004->15000/tcp, 0.0.0.0:9200->9200/tcp, 0.0.0.0:9300->9300/tcp
samsaradockerimages_grafana_1         /run.sh                          Up      0.0.0.0:15000->80/tcp
samsaradockerimages_influxdb_1        /run.sh                          Up      0.0.0.0:8083->8083/tcp, 8084/tcp, 0.0.0.0:8086->8086/tcp
samsaradockerimages_ingestion_1       /bin/sh -c /configure-and- ...   Up      0.0.0.0:15003->15000/tcp, 0.0.0.0:9000->9000/tcp
samsaradockerimages_kafka_1           /bin/sh -c /configure-and- ...   Up      0.0.0.0:15002->15000/tcp, 0.0.0.0:9092->9092/tcp
samsaradockerimages_kibana_1          /bin/sh -c /configure-and- ...   Up      0.0.0.0:15005->15000/tcp, 0.0.0.0:8000->8000/tcp
samsaradockerimages_qanal_1           /bin/sh -c /configure-and- ...   Up      0.0.0.0:15006->15000/tcp
samsaradockerimages_riemann_1         /bin/sh -c /start-supervis ...   Up      0.0.0.0:5555->5555/tcp, 5555/udp, 5556/tcp
samsaradockerimages_zookeeper_1       /bin/sh -c /configure-and- ...   Up      0.0.0.0:15001->15000/tcp, 0.0.0.0:2181->2181/tcp, 2888/tcp, 3888/tcp
```

To stop all services.

```
fig kill
```

### How to do manual bootstrap

An automatic bootstrap process should be triggered via the **bootstrap** container.
In case it fails of you want to set up the topics manually run the following commands.

```bash
fig run kafka bash
% /opt/kafka/bin/kafka-topics.sh --zookeeper $ZOOKEEPER_1_PORT_2181_TCP_ADDR --create --topic events --replication-factor 1 --partitions 5

% /opt/kafka/bin/kafka-topics.sh --zookeeper $ZOOKEEPER_1_PORT_2181_TCP_ADDR --describe

Topic:events	PartitionCount:1	ReplicationFactor:1	Configs:
	Topic: events	Partition: 0	Leader: 1	Replicas: 1	Isr: 1


# optionally start a console consumer
% /opt/kafka/bin/kafka-console-consumer.sh --zookeeper $ZOOKEEPER_1_PORT_2181_TCP_ADDR --topic events

% exit
```

On the docker host:

```bash
curl -XPUT 'http://localhost:9200/events-test' -d '{
    "settings" : {
        "number_of_shards" : 3,
        "number_of_replicas" : 0
    }
}'

curl -XPUT 'http://localhost:9200/kibana' -d '{
    "settings" : {
        "number_of_shards" : 1,
        "number_of_replicas" : 0
    }
}'
```


### How To build and push docker images

If you want to build and push all docker images with the right tags

```bash
# build the images
./build.sh

# to push to docker registry
# you need to login

docker login

# and finally push the images
./push.sh
```

**Please note:** that `qanal` and `ingestion-api` images are built
within their source projects.
Check [https://github.com/samsara](https://github.com/samsara) for
more info.

## License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)

