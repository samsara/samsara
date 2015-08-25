# Qanal

A stand alone indexer that reads messages (see Message Format below) from Kafka and bulk indexes them into a Elasticsearch Cluster.
This program is intended as an alternative to the [Elasticsearch Kafka River Plugin] (https://github.com/endgameinc/elasticsearch-river-kafka)

Tested against the Kafka-0.8.1.1 and Elasticsearch-1.4.4.

## Release Status
Still in development, hope to do the first release real soon.

## Continous Integration
Current build status: [![Circle CI](https://circleci.com/gh/samsara/qanal/tree/master.svg?style=shield)](https://circleci.com/gh/samsara/qanal/tree/master)


## Configuration
The configuration file is split into 3 sections.
  * kafka-source
  * elasticsearch-target
  * logging-options

The following is the provided example (config/config.edn)

```clojure
{:kafka-source
  {:zookeeper-connect  "127.0.0.1:2181"
   :connect-retry      5000
   :group-id	       "qanal"
   :auto-offset-reset  :earliest ; Can only be earliest or latest
   :fetch-size	       10485760	 ; size in bytes
   }

 ;; a map which lists all topic and partitions to consume from
 :topics
 [{:topic "events" :partitions :all :type :river}

  {:topic "twitter" :partitions [0 1 2 3 4 5 6 7 8 9]
   :type :plain
   :indexing-strategy :simple
   :index "social" :doc-type "twitter"
   }]

 :elasticsearch-target
   {:end-point "http://127.0.0.1:9200"}

 ;; metrics tracking
 :tracking
 {:enabled true :type :riemann
  :host "127.0.0.1"
  :port 5555}

 :logging-options
   {:min-level :info
    :path "qanal.log"			     ;full path name for the file
    :max-size 15360			     ;size in bytes
    :backlog 10}}
```

You can specify multitple topics and multiple partitions at the same time:

For example if you want to consume a topic called "twitter" which has
5 partition, and you want to consume all 5 partitions you can either
specify each individual partition as `[0 1 2 3 4]` or you can specify
`:all`.  If you just want to consume the first 3 partitions the you
can specify `[0 1 2]` this allow to split the load across multiple
instances where every instance will consume only one part of the
overall partitions.

The following configuration show that is possible to index multiple
topics at the same time.

```Clojure
...

 :topics
 [{:topic "events" :partitions :all :type :river}

  {:topic "twitter" :partitions [0 1 2 3 4 5 6 7 8 9]
   :type :plain
   :indexing-strategy :simple
   :index "social" :doc-type "twitter"}

  {:topic "identica" :partitions [0 2 4] ; just even partitions
   :type :plain
   :indexing-strategy :simple
   :index "social" :doc-type "twitter"
   }

  {:topic "wikipedia" :partitions :all
   :type :plain
   :indexing-strategy :daily
   :base-index "wikpedia" :doc-type "changes"
   :timestamp-field "timestamp" :timestamp-field-format :iso-8601
   :id-field "changeId"}]
   }
]

...
```

## Topics type: river

In the above example we are consuming 4 different topics. Let's analyse them
individually:

```
{:topic "events" :partitions :all :type :river}
```
This will consume *all* the partitions of the topic called `events`.
The `:river` type refers to the expected format of the data inside the topic.

### River Message Format


The messages sent to Kafka are expected to be json messages and in the
same format as those consumed by the Elasticsearch Kafka River Plugin.

```Javascript
	{
		"index" : "example_index",
		"type" : "example_type",
		"id" : "asdkljflkasjdfasdfasdf",
		"source" : { ..... }
	}
```

The `id` is optional while `index` and `type` are mandatory and they
refer to the Elasticsearch index and document type where the document
will be stored. The actual document content will be in the `source`
object. If an `id` it is present it will be used to identify the
document in the index, and in case another document is already present
with the same `id` it will be updated with the new values.  This is a
good way to avoid duplicates. If the `id` is not present or `null` a
auto-generated value will be used.

## Topics type: plain, indexing: simple

```Clojure

  {:topic "twitter" :partitions [0 1 2 3 4 5 6 7 8 9]
   :type :plain
   :indexing-strategy :simple
   :index "social" :doc-type "twitter"
   ;; optionally you can add a
   ;; :id-field "fieldname"
  }

```

When `:plain` is specified as type it is not required to wrap
the document like for the `:river` format, instead the document
will be indexed directly as it is.

The *indexing-strategy* `:simple` just indexes the document into
Elasticsearch in the index specified by `:index` and with the document
type specified by the `:doc-type` attribute.  If the documents have a
unique identifier it can be used for indexing by specifying the
`:id-field` attribute with the fieldname which contains the id.

This second example shows that not all partitions have to be consumed
but you can specify any subset. For example in this case we are
indexing only the partitions `[0 2 4]`.  This can be useful to
distribute the indexing on multiple machines for example in a
different host we could specify `[1 3 5]` to split the load between
the two hosts.

```Clojure
  {:topic "identica" :partitions [0 2 4] ; just even partitions
   :type :plain
   :indexing-strategy :simple
   :index "social" :doc-type "twitter"
   }

```

## Topics with daily indexing

```Clojure

  {:topic "wikipedia" :partitions :all
   :type :plain
   :indexing-strategy :daily
   :base-index "wikpedia" :doc-type "changes"
   :timestamp-field "timestamp" :timestamp-field-format :iso-8601
   :id-field "changeId"}]
   }

```

If you wish create daily indexes to store your document you need
to provide a `:base-index` name which will be used as prefix.
For example the indexes create from the above example will look
like the following:

   ...
   wikipedia-2015-08-23
   wikipedia-2015-08-24
   wikipedia-2015-08-25
   ...

the date will be rendered using the attribute specified by
`:timestamp-field`. This can be either:

  - `:system` which will use the current system date-time
  - or any attribute name contained in the document which contains a
    timestamp or a date-time

In the configuration snippet we are using the date contained in the
field called `timestamp` and we expect the date to the in the format
specified by the `:timestamp-field-format` attribute which it can be
one of:

  - `:millis` for the time elapsed in milliseconds from EPOC (same as `System.currentTimeMillis()`)
  - `:iso-8601` for a timestamp in the standard javascript ISO date such as `"2011-12-19T15:28:46.493Z"`
  - or finally you can specify a string which defines the format fo the date such as: `"YYYY-MM-dd"` [full format spec available here](http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html)

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
