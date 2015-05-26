# samsara-core

Real-time event stream processing pipeline for Samsara Analytics

## How to build it

This projects uses [lein-bin plugin](https://github.com/Raynes/lein-bin) which
is required to be installed.

```
lein do clean, bin
```

The `lein-bin` will produce an executable uberjar in `./target/samsara-core`.

## How to run it

```
-----------------------------------------------
   _____
  / ___/____ _____ ___  _________ __________ _
  \__ \/ __ `/ __ `__ \/ ___/ __ `/ ___/ __ `/
 ___/ / /_/ / / / / / (__  ) /_/ / /  / /_/ /
/____/\__,_/_/ /_/ /_/____/\__,_/_/   \__,_/

  Samsara CORE
-----------------------------| v0.1.0- |-------

SYNOPSIS
       ./samsara-core -c config.edn

DESCRIPTION
       Starts a processing pipeline node which will start
       consuming messages from the Ingestion-API sent to
       Kafka and produce a richer stream of event back
       into Kafka.

  OPTIONS:

  -c --config config-file.edn [REQUIRED]
       Configuration file to set up the processing node.
       example: './config/config.edn'

  -h --help
       this help page

```

The configuration is a EDN file with the following format.

```Clojure
{
  :job-name "Samsara"
  :input-topic "ingestion"
  :output-topic "events"
  ;; a CSV list of hosts and ports (and optional path)
  :zookeepers "127.0.0.1:2181"
  ;; a CSV list of host and ports of kafka brokers
  :brokers "127.0.0.1:9092"
  :offset :smallest
}
```



## License

Copyright © 2015 Samsara's authors

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
