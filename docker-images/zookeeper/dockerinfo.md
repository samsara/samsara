# Zookeeper installation

Runs a Zookeeper node. Designed to work in a cluster. Compatible with Kubernetes.
When working with Kebernetes it will use its discovery api to find the IPs of the ensemble.

Github project: [https://github.com/samsara/samsara](https://github.com/samsara/samsara/tree/master/docker-images/zookeeper)

## Dependencies

Zookeeper has no external dependencies

## Ports exposed

| Port  | Description                    |
|-------|--------------------------------|
|  2181 | Zookeeper client port          |
|  2888 | Zookeeper peers port           |
|  3888 | Zookeeper leader election port |
| 15000 | Supervisor web console         |

## Volumes used

* `/logs` for application logs.
* `/data` for zookeeper data (snapshot and db-logs).

## Configurable options

* `$ZK_SERVER_ID` : **REQUIRED**
A numerical ID of this Zookeeper node.
You have to ensure that the same ID isn't used by any other node in the cluster.

* `$ADV_IP`:
Is the IP address to advertise to the other nodes and clients.
It defaults to the container IP but you can use this property
to use the HOST ip instead.

* `HOSTNAME`: (default: `zk${ZK_SERVER_ID}`)
Hostname used while publishing metrics to Riemann.

When using with Kubernetes:

* `$DISCOVERY_SELECTOR`: (default: `zookeeper`)
This is the selector used to discover the other node in the ensemble.

* `$DISCOVERY_ENSEMBLE_MIN_SIZE`: (default: `3`)
How many zookeeper nodes have to be present to start the ensemble.
This is particularly important as the number of node in the cluster
must be carefully managed.

## Usage

```
docker run -d -p 2181:2181 -p 2888:2888 -p 3888:3888 -p 15000:15000 \
        -v /tmp/zookeeper/logs:/logs \
        -v /tmp/zookeeper/data:/data \
        -e ZK_SERVER_ID=1 \
        samsara/zookeeper
```
## Versions

* Samsara-`0.5.5.0`, `zk-3.4.6` - Zookeeper installation
* Samsara-`0.x.x.x`, `zk-3.5.1-alpha` - Zookeeper installation


## Copyright & License

Copyright © 2015-2016 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
