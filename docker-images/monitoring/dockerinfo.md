# Samsara monitoring

Monitoring system for Samsara Analytics

Github project: [https://github.com/samsara/samsara](https://github.com/samsara/samsara/tree/master/docker-images/monitoring)

## Ports exposed

| Port  | Description            |
|-------|------------------------|
|    80 | Grafana web            |
|  5555 | Riemann (tcp & udp)    |
|  5556 | Riemann websockets     |
|  8083 | InfluxDb admin         |
|  8086 | InfluxDb api           |
| 15000 | Supervisor web console |

## Volumes used

* `/logs` for application logs.
* `/data` for data

## Configurable options

* `HTTP_USER`: (default `admin`)
The user to be used to access Grafana dashboard

* `HTTP_PASS`: (default `samsara`)
The password to be used to access Grafana dashboard


## Usage

```
docker run -d -p 15000:80 -p 5555:5555 \
        -v /tmp/logs/monitoring:/logs \
        -v /tmp/data/monitoring:/data \
        samsara/monitoring
```

## Versions

* `0.5.5.0`  - Riemann `0.2.9`, InfluxDB `0.8.8`, Grafana `1.9.1`, Ubuntu `14.10`
* `0.5.7.0`  - Riemann `0.2.10`, InfluxDB `0.9.6.1`, Grafana `2.6.0`, MySQL `5.7.10`, Ubuntu `15.10`
* `0.5.7.1`  - Riemann `0.2.10`, InfluxDB `0.10.0-1`, Grafana `2.6.0`, MySQL `5.7.10`, Alpine Linux `3.3`


## Copyright & License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
