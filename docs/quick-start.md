---
layout: page
title: Quick Start
subtitle: Get started quickly with ready-made docker images.
nav: documentation
---

Samsara uses several third-party components, such as Apache ZooKeeper,Apache Kafka, ElasticSearch, Kibana etc.
Samsara provides production quality Docker images for those components to help you get started quickly.

### How to start a local environment

A local environment is ideal for development and testing purposes.
It setup a fully running cluster with all components but without
fault tolerance.

Please make sure you have latest [`docker`](https://docs.docker.com/)
and [`docker-compose`](https://docs.docker.com/compose/install/)
installed.

```bash
git clone https://github.com/samsara/samsara.git

cd samsara/docker-images
docker-compose pull
```

Now to start the services:

```
docker-compose up -d

# wait for all components to come up

# check the status with
docker-compose ps

```

Once the service is up and running you can then access the following
main services

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

Next try pushing a few events: 

**NOTE:** again if you are running on Mac the host will be your docker
vm (typically **192.168.59.103**)

``` bash
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

A successful output will look like:

``` bash
HTTP/1.1 202 Accepted
Content-Length: 0
Server: http-kit
Date: Mon, 11 Jan 2016 06:54:10 GMT

```

Next you can connect to kibana and see your events:

In your browser open http://localhost:8000/ (or http://192.168.59.103:8000/ for Mac).

Here you will be presented with the Kibana' setup page.
Set the following options:

  * check `Index contains time-based events`
  * enter `events*` in the `Index name or pattern` field
  * from the drop-down `Time-field name` select `ts`
  * Press `Create`

<img src="/docs/images/kibana-setup.png" alt="Kibana setup" width="400px"/>

A new page will appear showing the current mapping of the index, next
you can click `Discover` to visualize your events. By clicking on the
clock on the top-right corner you will be able to change the time
range and activate an `auto-refresh` of 5-10 seconds.

Now you should be able to see your events and as you push new events
you should be able to see the new ones too.

Finally, to stop all services.

```
docker-compose kill
```

