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

Now to start the services:

```
fig up -d

# wait for all components to come up

# check the status with
fig ps

```

Once the service is up and running you can then access
the following main services

| service       |          port          |
|---------------|------------------------|
| ingestion-api |  http://127.0.0.1:9000 |
| kibana        |  http://127.0.0.1:8000 |
| graphana      | http://127.0.0.1:15000 |

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

## License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)

