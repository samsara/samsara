---
layout: page
title: Getting started with development
subtitle:
nav: documentation
---

The default implementation of Samsara CORE doesn't do any processing
beside adding an id to every event.

To implement your custom processing you have to follow these steps:

```
lein new samsara my-streaming-app
```

This will create a project with the default structure:

```
my-streaming-app/
├── CHANGELOG.md
├── LICENSE
├── README.md
├── config
│   └── config.edn
├── doc
│   └── intro.md
├── docker-compose.yml
├── project.clj
├── resources
├── src
│   └── my_streaming_app
│       ├── core.clj
│       └── main.clj
└── test
    └── my_streaming_app
        └── core_test.clj
```

The `project.clj` should look like the following:

``` clojure
(defproject my-streaming-app "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [samsara/samsara-core "0.5.5.0"]]

  :main my-streaming-app.main

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-binplus "0.4.1"]]}}
  )
```

Please note the Samsara dependency, you can see all available version [Clojars](https://clojars.org/samsara/samsara-core).

Create a `main.clj` file which will start the processing part.

``` clojure
(ns my-streaming-app.main
  (:require [my-streaming-app.core :refer :all]
            [samsara-core.main :as sam])
  (:gen-class))


(defn -main [config-file]
  (println "Starting streaming processing.")
  (sam/start-processing! (sam/init! config-file)))
```

The config file `config/config.edn` looks like follow:

``` clojure
{:streams
 [{:id           :ingestion
   :input-topic  "ingestion"
   :state        :partitioned
   :output-topic "events"
   :processor    "my-streaming-app.core/make-processor"
   }]

 :job
 {:job-name "my-streaming-app"
  ;; a CSV list of hosts and ports (and optional path)
  :zookeepers "127.0.0.1:2181"
  ;; a CSV list of host and ports of kafka brokers
  :brokers "127.0.0.1:9092"
  :offset :smallest
  ;; this is useful only for local development
  :samza-overrides { :task.checkpoint.replication.factor 1 }
  }

 :tracking {:enabled true :type :console
            :reporting-frequency-seconds 600}

 }
```

The `:streams` section contains a list of stream which must be
consumed.  The default input is from a topic called `ingestion` and
the processed output goes to a topic called `events`.

The development endpoints for `:zookeeper` and Kafka `:brokers` will
be in your local machine. The `:samza-overrides` this is required only
for local development.

Similarly, the `:tracking` for development purposes can be enabled
on the console, but for a production environment you might want to publish
all metrics to the monitoring machine. More detail on this is available
on [Samsara's TRACKit!](https://github.com/samsara/trackit) project.


Now let's create a the processing functions:

``` clojure
(ns my-streaming-app.core
  (:require [samsara-core.core :as sam] ; default pipeline
            [moebius.core :refer :all]  ; processing functions
            [moebius.kv :as kv]         ; state management
            ))

```

We require the samsara CORE and the moebius for the processing functions:

now we can create our enrichment, filtering and correlation functions:

``` clojure

;;
;; Enrichment example.
;; You can compute any additional field and inject it
;; directly into the event.
;;
(defenrich game-name
  [event]
  (assoc event :game-name "Apocalypse Now"))


;;
;; Filtering example.
;; You can tell the pipeline to discard events
;; which match a particular condition.
;;
(deffilter no-ads [{:keys [eventName]}]
  (not= eventName "game.ad.displayed"))

;;
;; Correlation example.
;; Based on the events you are processing
;; you can produce new events
;;
(defcorrelate new-player
  [{:keys [eventName level timestamp sourceId] :as event}]

  (when (and (= eventName "game.started")
             (= level 1))
    [{:timestamp timestamp :sourceId sourceId :eventName "game.new.player"}]))

```

At this point we are able to compose our functions into a *pipeline* as follow:

``` clojure

;;
;; Pipelines.
;; Finally you can compose your pipelines
;; chaining your processing functions in the order
;; you wish process them.
;;
(def my-pipeline
  (pipeline
   (sam/make-samsara-pipeline {})
   game-name
   no-ads
   new-player))


;;
;; Finally you can produce a moebius
;; function which is it used by Samsara-CORE
;; to process incoming events.
;;
(defn make-processor [config]
  (moebius my-pipeline))
```

That's all we need to create our custom processing functions.

To see more about the processing functions read the [Develop your stream processing pipelines](development/stream-processing.md) page.

### Testing

If you wish to test your function you can use any of the standard Clojure testing framework such as [midje](https://github.com/marick/Midje).

Here is an example of testing:

``` clojure
(ns my-streaming-app.core-test
  (:require [my-streaming-app.core :refer :all]
            [midje.sweet :refer :all]))


(fact "ENRICHMENT: the game-name must be injected into all events"
      (game-name
       {:eventName "game.started"
        :timestamp 1430760258401
        :sourceId "device1"
        :level 1})
      => (contains {:game-name "Apocalypse Now"}))



(fact "FILTERING: filtering should drop game.ad.displayed"

      ;; no surprise here the predicates work like in filter function
      (no-ads {:eventName "game.level.completed"
               :timestamp 1430760258403
               :sourceId "device1"
               :levelCompleted 1})
      => true

      ;; when it doesn't match `false` or `nil` is returned
      (no-ads  {:eventName "game.ad.displayed"
                :timestamp 1430760258402
                :sourceId "device1"})
      => false)



(fact "CORRELATION: when a game.started event if found with a level=1,
       we could infer that a new player started play with our game."

      (new-player {:eventName "game.started"
                   :timestamp 1430760258401
                   :sourceId "device1"
                   :level 1})
      =>
      [{:timestamp 1430760258401, :sourceId "device1", :eventName "game.new.player"}]

      (new-player {:eventName "game.started"
                   :timestamp 1430760258401
                   :sourceId "device1"
                   :level 5})
      => nil)

```

## How to build, test and run.

Build with:

     lein do clean, midje, bin

This will build a executable jar using the [lein-binplus](https://github.com/BrunoBonacci/lein-binplus) plugin.

The easiest way to run it locally is to create a local development cluster with `docker-compose`. Here is a sample file:
**NOTE: in ADV_IP you are required to put your machine's local ip address but not the localhost (127.0.0.1) to enable
your processing job to communicate with Zookeeper and Kafka.**

```
#
# Zookeeper
#
zookeeper:
  image: samsara/zookeeper:snapshot
  ports:
    - "2181:2181"
    - "15001:15000"
  environment:
    ZK_SERVER_ID: 1
    # Your ip but NOT 127.0.0.1
    ADV_IP: "192.168.0.2"
  volumes:
    - /tmp/logs/zk1:/logs
    - /tmp/data/zk1:/data

#
# Kafka
#
kafka:
  image: samsara/kafka:snapshot
  ports:
    - "9092:9092"
    - "15002:15000"
  links:
    - zookeeper:zookeeper
  environment:
    KAFKA_BROKER_ID: 1
    # Your ip but NOT 127.0.0.1
    ADV_IP: "192.168.0.2"
  volumes:
    - /tmp/logs/kafka1:/logs
    - /tmp/data/kafka1:/data

#
# Samsara Ingestion API
#
ingestion:
  image: samsara/ingestion-api:snapshot
  links:
    - kafka:kafka
    - monitoring:riemann
  ports:
    - "9000:9000"
    - "15003:15000"
  environment:
    OUTPUT_TOPIC: "ingestion"
    TRACKING_ENABLED: "true"
  volumes:
    - /tmp/logs/ingestion-api:/logs

#
# Samsara CORE
#
#core:
#  image: myuser/my-streaming-app
#  links:
#    - kafka:kafka
#    - zookeeper:zookeeper
#    - monitoring:riemann
#  ports:
#    - "15010:15000"
#  environment:
#    TRACKING_ENABLED: "true"
#    SINGLE_BROKER: "true"
#  volumes:
#    - /tmp/logs/core:/logs


#
# ElasticSearch
#
elasticsearch:
  image: samsara/elasticsearch:snapshot
  links:
    - zookeeper:zookeeper
  ports:
    - "9200:9200"
    - "9300:9300"
    - "15004:15000"
  volumes:
    - /tmp/logs/els:/logs
    - /tmp/data/els:/data

#
# Kibana
#
kibana:
  image: samsara/kibana:snapshot
  links:
    - elasticsearch:elasticsearch
  ports:
    - "8000:8000"
    - "15005:15000"
  volumes:
    - /tmp/logs/kibana:/logs

#
# Samsara Qanal
#
qanal:
  image: samsara/qanal:snapshot
  links:
    - zookeeper:zookeeper
    - elasticsearch:els
    - monitoring:riemann
  ports:
    - "15006:15000"
  environment:
    TRACKING_ENABLED: "true"
    KAFKA_TOPICS_SPEC: |
      { :topic "events" :partitions :all :type :plain
        :indexing {:strategy :simple :index "events" :doc-type "events" :id-field "id"}}
  volumes:
    - /tmp/logs/qanal1:/logs

#
# Monitoring
#
monitoring:
  image: samsara/monitoring:snapshot
  ports:
    - "15000:80"
    - "5555:5555"
    - "5556:5556"
    - "8083:8083"
    - "8086:8086"
  environment:
    HTTP_USER: admin
    HTTP_PASS: samsara
  volumes:
    - /tmp/logs/monitoring:/logs
    - /tmp/data/monitoring:/data


#
# Bootstrap
#
bootstrap:
  image: samsara/kafka:snapshot
  links:
    - zookeeper:zookeeper
    - kafka:kafka
    - elasticsearch:elasticsearch
  command: bash -c "curl -sSL 'https://raw.githubusercontent.com/samsara/samsara/master/docker-images/bootstrap/bootstrap.sh' | bash"
  volumes:
    - /tmp/logs/bootstrap:/logs
```

Please **NOTE**: you have to replace the **ADV_IP** with your local
machine ip address.

Then run:

     docker-compose up

and when you see the following message appearing on the console:

```
bootstrap_1      |
bootstrap_1      | ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
bootstrap_1      | ;;                                                                            ;;
bootstrap_1      | ;;    ---==| S A M S A R A   I S   R E A D Y   F O R   A C T I O N |==----    ;;
bootstrap_1      | ;;                                                                            ;;
bootstrap_1      | ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
bootstrap_1      |
```

The you can start your streaming processing core:


    ./target/my-streaming-app-0.1.0-SNAPSHOT config/config.edn

This should startup and connect to Kafka and Zookeeper and when you see:

```
INFO [samsara-core.main] - Processed     0 events at     0/s
INFO [samsara-core.main] - Processed     0 events at     0/s
INFO [samsara-core.main] - Processed     0 events at     0/s
INFO [samsara-core.main] - Processed     0 events at     0/s
INFO [samsara-core.main] - Processed     0 events at     0/s

```

You are ready to publish events to the ingestion-api, like:

``` bash
cat <<EOF | curl -i -H "Content-Type: application/json" \
                -H "X-Samsara-publishedTimestamp: $(date +%s999)" \
                -XPOST "http://127.0.0.1:9000/v1/events" -d @-
[
  {
    "timestamp": $(date +%s000),
    "sourceId": "test-device",
    "eventName": "game.started",
    "level": 1,
    "levelScore": $RANDOM
  }
]
EOF
```

Finally you should be able to see the events via [http://217.0.0.1:8000](http://217.0.0.1:8000).
The first time you'll need to set it up as described in [Quick start guide](http://samsara-analytics.io/docs/quick-start/).
