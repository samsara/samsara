---
layout: page
title: Samsara Go Client
subtitle: Go Client for Samsara.
nav: documentation
tab_bar: clients
---

## Usage

To use the Samsara client, install it by `go get` command

```bash
$ go get github.com/samsara/samsara/clients/go
```

In your code:

```go
import "github.com/samsara/samsara/clients/go"
```

And then to create client instance:

```go
config := client.NewConfig()
config.Url = "http://my.samsara.server:9000/"
config.SourceId = "source identifier"

myClient, err := client.NewClient(config)
if err != nil {
  // log error.
}
```
It creates a Samsara Client instance. Now you can publish events to Samsara!

Note, that URL is a required configuration option and must be provided.

If any of the configuration options is invalid `ConfigValidationError` will be returned.

### Record and Publish an Event

Samsara SDK buffers events and publishes them to the Ingestion API
periodically. To record an event in the Samsara buffer:


```go
event := []client.Event{ "eventName": "user.logged", "sourceId": "device1", "timestamp": int64(1479988864057) }
myClient.RecordEvent(event)
```

Or you can use `client.Timestamp()` method to generate a correct timestamp in milliseconds suitable for Samsara.

```go
event := []client.Event{ "eventName": "user.logged", "sourceId": "device1", "timestamp": client.Timestamp() }
myClient.RecordEvent(event)
```

If the `sourceId` is not provided then the one set in the configuration will be used.
If the `timestamp` is not provided then client automatically will generate current system time in milliseconds.
This means that you can record event just by:

```go
event := []client.Event{ "eventName": "user.logged" }
myClient.RecordEvent(event)
```
Additionally you can provide your own key-value pairs:
Remember that Event is just a type synonym for `map[string]interface{}`

```go
event := []client.Event{ "eventName": "user.logged", "color": "blue", "level": 10 }
myClient.RecordEvent(event)
```

`PublishEvents` stores events in a thread-safe buffer so you can use it in several goroutines, if needed.

Alternatively, you can publish a bulk of events immediately to the
Ingestion API using the `PublishEvents` method.

```go
data := []client.Event{
    {
      "eventName": "user.logged-in",
      "sourceId":  "mobile",
    },
    {
      "eventName": "user.logged-out",
      "sourceId":  "mobile",
    },
  }
myClient.PublishEvents(data)
```

The events will be sent to Samsara immediately.
Note that `PublishEvents` method signature is `[]Event` so if you want to send only one event
it should be wrapped in array as well.

Also please note that `RecordEvent` and `PublishEvents` can raise `EventValidationError`
if any of the given events doesn't conform Event specification.

```go
data := []client.Event{
    {
      "eventName": "user.logged-in",
      "sourceId":  "mobile",
      "timestamp": "some-incorrect-data"
    },
    {
      "eventName": "user.logged-out",
      "sourceId":  "mobile",
    },
  }
success, err := myClient.RecordEvent(data)
if err != nil {
  // our data contains incorrect events
}
if success == false {
  // events may be valid, but post to Ingestion API failed
}
```

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

Samsara SDK buffers events and periodically flushes events to the
Samsara API. `Circular buffer` implementation is used for this purpose. The events are
removed from the buffer only if the publish was successful. Newer
events overwrite the oldest ones after buffer reaches its
capacity.

The interval for events publishing and the maximum buffer size can
be configured as well. Note that configuration can not
be changed once the client has been initialized. Any changes will get reflected when
the client is restarted.

```go
type Config struct {
  // Samsara ingestion api endpoint "http://samsara-ingestion.local/"
  Url string

  // Identifier of the source of these events.
  // OPTIONAL used only for record-event
  SourceId string

  // Start the publishing thread?
  // default = true
  StartPublishingThread bool

  // How often should the events being sent to Samsara
  // in milliseconds.
  // default = 30s
  PublishInterval uint32

  // Max size of the buffer.
  // When buffer is full older events are dropped.
  MaxBufferSize int64

  // Minimum number of events that must be in the buffer
  // before attempting to publish them.
  MinBufferSize int64

  // Network timeout for send operations
  // in milliseconds.
  // default 30s
  SendTimeout uint32

  // Should the payload be compressed?
  // allowed values: "gzip", "none"
  Compression string

  // NOT CURRENTLY SUPPORTED
  // Add Samsara client statistics events
  // this helps you to understand whether the
  // buffer size and publish-intervals are
  // adequately configured.
  // SendClientStats bool
}
```

## License

Copyright © 2017 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
