---
layout: page
title: Samsara Clojure Client
subtitle: Clojure Client for Samsara.
nav: documentation
tab_bar: clients
---

## Usage

To use the Samasara SDK you need to add the following dependency to
your `project.clj` file.

```clojure
[samsara/samsara-client "0.2.0"]
```

Load the namespace in the REPL

```clojure
(require '[samsara.client :as samsara])
```
or as part of your namespace

```clojure
(ns my.project
    (:require [samsara.client :as samsara]))
```

To set your configuration with:

```clojure
(samsara/init! {:url "http://my.samsara.server:9000/"
                :sourceId "the identifier of the given source"})
```

Now you can start to publish events to samsara.

### Publish an Event

Samsara SDK buffers events and publishes them to Ingestion API
periodically. To record an event in the Samsara buffer do

```clojure
(samsara/record-event! {:eventName "user.logged" :sourceId "device1" :timestamp 1234567890})
```
If the `sourceId` is not provided then the one set int the configuration will be used.
If the `timestamp` is not provided then it will be used the current system time.
So this mean that you can record a event just as follow:

```clojure
(samsara/record-event! {:eventName "user.logged"})
```

Additionally you can provide your own key-value pairs:

```clojure
(samsara/record-event! {:eventName "user.logged" :color "blue" :level 10})
```

Alternatively, you can publish bulk events immediately to the
Ingestion API using the publish-events function.

```clojure
(samsara/publish-events [{:eventName "user.logged"
                          :timestamp 1431261991023
                          :sourceId "e6f01efd-04a9-4c18-a210-2806718b6d43"})]
```
The event will be sent to samsara immediately.


### SourceID

The sourceId must be provided. **It is important to select carefully
your client ID, as all events with the same `sourceId` will be routed
to the same server and same thread.**.  This property is important to
provide linearizability of events coming from the same client.

In order to select a good `sourceId` you have to look for the
following properties.

  - **high cardinality** - which will help the system to scale
  - **non randomized** - the same id must be used by the same source over time.
  - **unique** - You must be able to identify uniquely a source from a given ID.

Here some examples of **good** choices for `sourceId`:

  - a device id for a mobile application
  - a customer or client id for a web application
  - a userid for a web service

Here some examples of **BAD** bad choices for `sourceId`:

  - a web service name, because it is a low cardinality. This means
    that all events coming from a particular webservice will be
    processed by a single thread.  A busy webservice can do hundreds
    of millions or billions of requests per day, which in this case
    will be all queued to be processed by a single thread.  Replace
    the webservice name with the clientId of the webservice user, or
    the sessionId or if you really want to use name append the process
    id (PID) to the name (such as:
    "com.example.api.user-service:56789")

  - a randomly generate id which is not persitsed and regenerated on
    every use.  This is bad because it doesn't allow you to trace an
    history of the events and make meaningful correlations.

  - same it will happen if the sourceId is not unique. Events from
    multiple different sources will mix together generating an
    undistiguishable events soup


### Advanced configuration

Samsara SDK buffers events and peridically flushes the events to the
Samsara API. A ring buffer is used for this purpose. The events are
removed from the buffer only if the publish was successful. Newer
events overwrite the oldest events when the buffer reaches its
capacity.

The interval to publish events and the maximum ring buffer size can
also be configured. Note that at this point the configuration cannot
be changed once it is initialised. Any changes will get reflected when
the app restarts.

```Clojure
  {
   ;; a samsara ingestion api endpoint  "http://samsara.io/"
   ;; :url  - REQUIRED

   ;; the identifier of the source of these events
   ;; :sourceId  - OPTIONAL only for record-event

   ;; whether to start the publishing thread.
   :start-publishing-thread true

   ;; how often should the events being sent to samsara
   ;; in milliseconds
   ;; default 30s
   :publish-interval 30000

   ;; max size of the buffer, when buffer is full,
   ;; older events are dropped.
   :max-buffer-size  10000

   ;; minimum number of events to that must be in the buffer
   ;; before attempting to publish them
   :min-buffer-size 100


   ;; network timeout for send operaitons (in millis)
   ;; default 30s
   :send-timeout-ms  30000

   ;; whether of not the payload should be compressed
   ;; allowed values :gzip :none
   :compression :gzip

   ;; add samsara client statistics events
   ;; this helps you to understand whether the
   ;; buffer size and publish-intervals are
   ;; adequately configured.
   ;; :send-client-stats true
   })
```

## Component support

This library supports Stuart Sierra Component library and provide component.
In order to use this library as a component just add it to your system:

```Clojure
(component/system-map
  :samsara (samsara/samsara-client configuration)
  ,,,,)
```

And then pass the component to the `record-event!` function.

```
(samsara/record-event! component {:eventName "user.logged"})
```
