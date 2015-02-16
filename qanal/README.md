# Qanal

A stand alone indexer that reads messages (see Message Format below) from Kafka and bulk indexes them into a Elasticsearch Cluster.  
This program is intended as an alternative to the [Elasticsearch Kafka River Plugin] (https://github.com/endgameinc/elasticsearch-river-kafka)

It should work against the 0.8.1.1 release of Kafka  
Current build status: [![Build Status](https://travis-ci.org/samsara/qanal.svg?branch=master)](https://travis-ci.org/samsara/qanal.svg?branch=master)

## Status
Still in development, hope to do the first release real soon.

## Configuration
The configuration file is split into 3 sections.
* kafka-source
* elasticsearch-target
* logging-options

The following is the provided example (resources/config.edn)
```clojure
{:kafka-source {:zookeeper-connect  "127.0.0.1:49157"
                :connect-retry      5000
                :group-id           "Qanal"
                :topic "river"
                :partition-id 0
                :auto-offset-reset  :earliest ; Can only be earliest or latest
                :fetch-size         10485760                ;size in bytes
                }
 :elasticsearch-target {:end-point "http://127.0.0.1:9200"}
 :logging-options {:min-level :info
                   :path "Qanal.log"                        ;full path name for the file
                   :max-size 15360                          ;size in bytes
                   :backlog 10}}
```

## Message Format (Kafka)
The messages sent to Kafka are expected to be json messages and in the same format as those
consumed by the Elasticsearch Kafka River Plugin.

	{
		"index" : "example_index",
		"type" : "example_type",
		"id" : "asdkljflkasjdfasdfasdf",
		"source" : { ..... }
	}
## Usage

To run the program, build an uberjar and run the jar

    $ lein do clean, uberjar
    $ java -jar target/qanal-0.1.0-standalone.jar -c /opt/qanal/config.edn

## License

Copyright © 2015 FIXME

Distributed under the The MIT License (MIT).
