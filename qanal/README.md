# Qanal

A stand alone indexer that reads messages (see Message Format below) from Kafka and bulk indexes them into a Elasticsearch Cluster.
This program is intended as an alternative to the [Elasticsearch Kafka River Plugin] (https://github.com/endgameinc/elasticsearch-river-kafka)

Tested against the Kafka-0.8.1.1 and Elasticsearch-1.4.4.

## Release Status
Still in development, hope to do the first release real soon.

## Continous Integration
Current build status: [![Build Status](https://travis-ci.org/samsara/qanal.svg?branch=master)](https://travis-ci.org/samsara/qanal.svg?branch=master)


## Configuration
The configuration file is split into 3 sections.
* kafka-source
* elasticsearch-target
* logging-options

The following is the provided example (resources/config.edn)
```clojure
{:kafka-source {:zookeeper-connect  "127.0.0.1:49157"
                :connect-retry      5000
                :group-id           "qanal"
                :topic "river"
                :partition-id 0
                :auto-offset-reset  :earliest ; Can only be earliest or latest
                :fetch-size         10485760                ;size in bytes
                }
 :elasticsearch-target {:end-point "http://127.0.0.1:9200"}
 :logging-options {:min-level :info
                   :path "qanal.log"                        ;full path name for the file
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

To run the program, build an executable uberjar

    $ lein do clean, bin
    $ ./target/qanal -c /opt/qanal/config.edn

## Committers

  * Dayo Oliyide  ([@DayoOliyide](https://github.com/DayoOliyide))
  * Bruno Bonacci ([@BrunoBonacci](https://github.com/BrunoBonacci))

## License

Copyright © 2015 Samsara's team

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
