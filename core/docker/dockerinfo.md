# Samsara Core

Starts a processing pipeline node which will start consuming messages from the Ingestion-API sent to Kafka and produce a richer stream of event back into Kafka.

## Ports exposed

| Port  | Description             |
|-------|-------------------------|
| 4500  | nREPL                   |
| 15000 | Supervisord web console |



## Dependencies
  - 1 or more Kafka brokers **REQUIRED**
  - 1 or more Zookeepers **REQUIRED**
  - Reinmann **REQUIRED if TRACKING_ENABLED**

## Volumes used

*  `/logs` for application logs


## Configurable options

* `KAFKA_1_PORT_9092_TCP_ADDR` .. `KAFKA_n_PORT_9092_TCP_ADDR` : **REQUIRED*
The IP address of the Kafka broker, you can specify more than one broker.
This also supports the Docker link format.

* `ZOOKEEPER_1_PORT_2181_TCP_ADDR` .. `ZOOKEEPER_n_PORT_2181_TCP_ADDR` : **REQUIRED*
The IP address of the Zookeeper client, you can specify more than one broker.
This also supports the Docker link format.

* `SAMZA_CONFIG`: (default `""`)
Configuration options to override default Samza options.

* `SINGLE_BROKER`: (default `false`)
A flag for whether to use a single kafta broker or not

* `INPUT_TOPIC`: (default: `ingestion`)
The kafka topic where the events are ingested.

* `INPUT_PARTITIONS`: (default `:all`)
The kafta topic partitions to ingest from.

* `JOB_NAME`: (default `Samsara`)
The name of the job to be executed.

* `OFFSET_RESET`: (default `:smallest`)
The topic's log offset to start fetching from, can be in `:smallest` or `:largest`.

* `TRACKING_ENABLED` (default: `false`)
Whether the system should send tracking metrics to Riemann.

* `RIEMANN_PORT_5555_TCP_ADDR` (default: `localhost`)
If the `TRACKING_ENABLED` is true, then the IP of a Riemann.

* `RIEMANN_PORT_5555_TCP_PORT` (default: `5555`)
The Riemann port where to send all the metrics.

* `HOSTNAME` (default: `samsara-core`)
The prefix to use in Reimann, prepended with `samsara.core.`

## Usage

```
# it is important that you pass the `-i` option to create a stdin chanel
docker run -tdi -p 15000:15000 -v /tmp/samsara-core:/logs \
	--link=samsaradockerimages_kafka_1:kafka_1 \
	--link=samsaradockerimages_kafka_2:kafka_2 \
	--link=samsaradockerimages_kafka_3:kafka_3 \
	--link=samsaradockerimages_zookeeper_1:zookeeper_1 \
	--link=samsaradockerimages_zookeeper_2:zookeeper_2 \
	--link=samsaradockerimages_zookeeper_3:zookeeper_3 \
	samsara/samsara-core
```

## Copyright & License

Copyright © 2015-2017 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
