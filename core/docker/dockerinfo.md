# Samsara Core

Starts a processing pipeline node which will start consuming messages from the Ingestion-API sent to Kafka and produce a richer stream of event back into Kafka.

## Ports exposed

| Port  | Description             |
|-------|-------------------------|
| 15000 | Supervisord web console |


## Dependencies
  - Kafka broker **REQUIRED**
  - Zookeeper **REQUIRED**


## Volumes used

*  `/logs` for application logs


## Configuration

The configuration is a EDN file with the following format.

```
{:topics
	{:job-name "Samsara"
	 :input-topic "ingestion"
	 :output-topic "events"
	 ;; a CSV list of hosts and ports (and optional path)
	 :zookeepers "127.0.0.1:2181"
	 ;; a CSV list of host and ports of kafka brokers
	 :brokers "127.0.0.1:9092"
	 :offset :smallest}
}
```

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

Copyright © 2016 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
