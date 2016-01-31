# Samsara base image

Base image used for all Samsara's images.

Github project: [https://github.com/samsara/samsara](https://github.com/samsara/samsara/tree/master/docker-images/base)

## Ports exposed

No ports exposed

## Volumes used

* `/logs` for application logs.

## Configurable options

No configurable options

## Usage

```
docker run -d -v /tmp/logs/os:/logs samsara/base-image-jdk8
```

## Versions

* `samsara/base-image-jdk7` \| `0.5.5.0` \| `u1410-j7u75`
  - Ubuntu 14.10, JDK 1.7u75, supervisord, jq, os tools
* `samsara/base-image-jdk8` \| `0.5.5.0` \| `u1410-j8u40`
  - Ubuntu 14.10, JDK 1.8u40, supervisord, jq, os tools
* `samsara/base-image-jdk7` \| `0.5.6.0` \| `u1510-j7u79`
  - Ubuntu 15.10, JDK 1.7u75, supervisord, jq, os tools
* `samsara/base-image-jdk8` \| `0.5.6.0` \| `u1510-j8u71`
  - Ubuntu 15.10, JDK 1.8u71, supervisord, jq, os tools
* `samsara/base-image-jdk8` \| `0.5.x.x` \| `a33-j8u72`
  - Alpine Linux 3.3, JDK 1.8u72, supervisord, jq, os tools


## Copyright & License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
