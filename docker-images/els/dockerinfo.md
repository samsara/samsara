# Samsara ElasticSearch

A production ready installation of Apache ElasticSearch

Github project: [https://github.com/samsara/samsara-docker-images](https://github.com/samsara/samsara-docker-images)

## Ports exposed

| Port  | Description             |
|-------|-------------------------|
|  9200 | Default Client ELS port |
|  9300 | Default Peer ELS port   |
| 15000 | Supervisor web console  |

## Volumes used

* `/logs` for application logs.
* `/data` for data

## Configurable options

* `ZOOKEEPER_PORT_2181_TCP_ADDR`: **REQUIRED**
The IP address of the Zookeeper instance,
This is used for cluster membership and
leader election.

* `ELS_NODE_TYPE` : (default: `standard`)
Configures the `node.box_type` option

* `ELS_HEAP_SIZE`: (default: 1/2 of box available memory)
This configures the `-Xmx` Java option.
It is recommended to give some memory for the off-heap
data and the page-cache.

* `ADV_IP`: (default to conatiner ip)
The IP address to advertise to the clients

* `HOSTNAME`: (default: `ingestion`)
Hostname used while publishing metrics to Riemann.

* `AWS_ACCESS_KEY` (default: ``) _since: 1.6.2b_
If running on AWS you can specify the key to be used
for api access.

* `AWS_SECRET_KEY` (default: ``) _since: 1.6.2b_
If running on AWS you can specify the key to be used
for api access.

* `AWS_REGION` (default: `eu-west`) _since: 1.6.2b_
If running on AWS you can specify the region to connect
for api access.

* `AWS_REPOS_NAME` (default: `eu-west`) _since: 1.6.2b_
If using AWS/S3 for repository backups you can specify
the bucket to use for storing backups.

* `AWS_REPOS_ACCESS_KEY` (default: `$AWS_ACCESS_KEY`) _since: 1.6.2b_
If using AWS/S3 for repository backups you can specify
the key to be used for S3 access if different from `AWS_ACCESS_KEY`

* `AWS_REPOS_SECRET_KEY` (default: `$AWS_SECRET_KEY`) _since: 1.6.2b_
If using AWS/S3 for repository backups you can specify
the key to be used for S3 access  if different from `AWS_SECRET_KEY`

* `AWS_REPOS_BASE_PATH` (default: `/samsara`) _since: 1.6.2b_
If using AWS/S3 for repository backups you can specify
the base path under which the backups will be stored.


## Usage

```
docker run -d -p 9200:9200 -p 9300:9300 -p 15000:15000 \
        -v /tmp/logs/els:/logs \
        -v /tmp/data/els:/data \
        --link=zookeeperdocker_zookeeper_1:zookeeper \
        samsara/elasticsearch
```

## Versions

* `1.5.0`  - ELS `1.5.0` ready for production installation
* `1.5.2b` - ELS `1.5.2` patch update
* `1.6.2`  - ELS `1.6.2` Minor ELS version update
* `1.6.2b` - ELS `1.6.2b` Added AWS plugin



## Copyright & License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
