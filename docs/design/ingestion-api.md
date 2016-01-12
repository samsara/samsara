---
layout: page
title: Ingestion API
subtitle: Ingestion API.
nav: documentation
---

## Ingestion API

For installation instructutions refer to the Ingestion API project in [GitHub](https://github.com/samsara/samsara/ingestion-api).

For the format of the events please refer to [Events Specification](/docs/design/events-spec).

### Available endpoints

### /events

This endpoint accepts a set of events and sends it to the backend for further processing. The type of backend is determined by the configuration. This endpoint accepts JSON payloads and gzip compressed Json payloads. Here some request sample:

Example of a plain JSON payload:

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

Example of a GZIP JSON payload:

```
cat <<EOF | gzip | curl -i -H "Content-Type: application/json" \
                        -H "X-Samsara-publishedTimestamp: $(date +%s999)" \
                        -H "Content-Encoding: gzip" \
                        -XPOST "http://localhost:9000/v1/events" \
                        --data-binary @-
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

### /api-status

This endpoint controls the status of the services.

You can check the current status with:

```
curl -i -XGET "http://localhost:9000/v1/api-status"

{ "status": "online" }
```

You can change the current status with:

```
curl -i -H "Content-Type: application/json" \
     -XPUT "http://localhost:9000/v1/api-status" \
     -d '{ "status": "offline" }'
```

Where status can be one of: `online` or `offline`.
When set to `offline` the `GET /api-status` will return `HTTP 503` as response code.

This is useful for maintenance purposes to take one instance off of the loadbalancer.
