# Samsara Kafka

A production ready installation of Apache Kafka

Github project: [https://github.com/samsara/samsara](https://github.com/samsara/samsara/tree/master/docker-images/kafka)

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

* `KAFKA_BROKER_PROTOCOL_VERSION` (default: *empty*)
This is protocol version used by brokers for communication.
Leaving it empty will actually mean a default value of `0.10.0-IV1`
for kafka 0.10.0.0
This setting is mainly used when doing a rolling update of your cluster.
Please check the official [Kafka documentation](http://kafka.apache.org/documentation.html#upgrade) for more info.

* `KAFKA_MESSAGE_FORMAT_VERSION` (default: *empty*)
This is the format version that the messages are written in, when appending
to the kafka log.
Leaving it empty will actually mean a default value of `0.10.0-IV1`
for kafka 0.10.0.0
This setting is mainly used when doing a rolling update of your cluster, and
should be set to a value equal or less than the above `KAFKA_BROKER_PROTOCOL_VERSION`.
Please check the official [Kafka documentation](http://kafka.apache.org/documentation.html#upgrade)

* `KAFKA_BROKER_RACK` (default: *empty*)
The rack (for AWS, availability zone) that the broker is placed on.

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

* Samsara-`0.5.5.0`, `kafka-0.8.2.1` - Kafka with enabled compaction and topic delete
* Samsara-`0.x.x.x`, `kafka-0.10.1.0` - New Kafka version.


## Copyright & License

Copyright © 2016 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
