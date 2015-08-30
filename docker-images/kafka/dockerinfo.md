# Samsara Ingestion-API

A production ready installation of Apache Kafka

Github project: [https://github.com/samsara/samsara-docker-images](https://github.com/samsara/samsara-docker-images)

## Ports exposed

| Port  | Description            |
|-------|------------------------|
|  9092 | Default Kafka port     |
| 15000 | Supervisor web console |

## Volumes used

* `/logs` for application logs.
* `/data` for data

## Configurable options

* `KAFKA_BROKER_ID` : **REQUIRED**
This should be a number and should be unique across the cluster

* `ZOOKEEPER_PORT_2181_TCP_ADDR`: **REQUIRED**
The IP address of the Zookeeper instance

* `KAFKA_BROKER_PORT`: (default `9092`)
The port in which the broker should listen

* `ADV_IP`: (default to conatiner ip)
The IP address to advertise to the clients

* `HOSTNAME`: (default: `ingestion`)
Hostname used while publishing metrics to Riemann.

* `KAFKA_ENABLE_REPORTING_STATSD` (default: `false`)
Whether the system should send tracking metrics to Statsd

* `STATSD_PORT_8125_TCP_ADDR` (default: `localhost`)
If the `KAFKA_ENABLE_REPORTING_STATSD` is true, then the IP of a Statsd daemon.

## Usage

```
docker run -d -p 9092:9092 -p 15000:15000 \
        -v /tmp/logs/kafka:/logs \
        -v /tmp/data/kafka:/data \
        --link=zookeeperdocker_zookeeper_1:zookeeper \
        -e KAFKA_BROKER_ID=1
        samsara/kafka
```

## Versions

* `0.8.2.1`  - Kafka `0.8.2.1` ready for production installations
* `0.8.2.1b` - Kafka `0.8.2.1`
* `0.8.2.1c` - Kafka `0.8.2.1` with configurable advertised-ip


## Copyright & License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
