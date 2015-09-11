# Samsara Ingestion-API

Runs a RESTful ingestion API for the Samsara real-time analytics.

Api specification available [https://github.com/samsara/samsara-ingestion-api/blob/master/spec/ingestion-api-spec.yaml](https://github.com/samsara/samsara-ingestion-api/blob/master/spec/ingestion-api-spec.yaml).

## Dependecies
  - Kafka broker **REQUIRED**

## Ports exposed

| Port  | Description            |
|-------|------------------------|
|  9000 | REST API port          |
| 15000 | Supervisor web console |

## Volumes used

* `/logs` for application logs.

## Configurable options

* `KAFKA_1_PORT_9092_TCP_ADDR` .. `KAFKA_n_PORT_9092_TCP_ADDR` : **REQUIRED**
The IP address of the Kafka broker, you can specify more than one broker.
and supports the Docker link format.

* `OUTPUT_TOPIC`: (default: `ingestion`)
The kafka topic where the events must be sent.

* `HOSTNAME`: (default: `ingestion`)
Hostname used while publishing metrics to Riemann.

* `TRACKING_ENABLED` (default: `false`)
Whether the system should send tracking metrics to Riemann

* `RIEMANN_PORT_5555_TCP_ADDR` (default: `localhost`)
If the `TRACKING_ENABLED` is true, then the IP of a Riemann.

* `RIEMANN_PORT_5555_TCP_PORT` (default: `5555`)
The Riemann port where to send all the metrics.


## Usage

```
docker run -d -p 9000:9000 -p 15000:15000 -v /tmp/ingestion-api:/logs \
        --link=kafkadocker_kafka_11:kafka_1 \
        --link=kafkadocker_kafka_12:kafka_2 \
        --link=kafkadocker_kafka_13:kafka_3 \
        -e OUTPUT_TOPIC=ingestion
        samsara/ingestion-api
```

Then send events with:

```
cat <<EOF | curl -i -H "Content-Type: application/json" \
                -H "X-Samsara-publishedTimestamp: $(date +%s999)" \
                -XPOST "http://localhost:9000/v1/events" -d @-
[
  {
    "timestamp": $(date +%s000),
    "sourceId": "3aw4sedrtcyvgbuhjkn",
    "eventName": "user.item.added",
    "page": "orders",
    "item": "sku-1234"
  }, {
    "timestamp": $(date +%s000),
    "sourceId": "3aw4sedrtcyvgbuhjkn",
    "eventName": "user.item.removed",
    "page": "orders",
    "item": "sku-5433",
    "action": "remove"
  }
]
EOF
```

## Copyright & License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
