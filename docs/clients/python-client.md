---
layout: page
title: Samsara Python 3 Client
subtitle: Python 3 Client for Samsara.
nav: documentation
tab_bar: clients
---

## Usage

To use the Samsara SDK, pip install it via:

```bash
$ pip install samsara_sdk
```

To bring it into the repl:

```python
> from samsara_sdk import SamsaraClient
> # helper function which return the current time in milliseconds.
> from samsara_sdk.helpers import current_time_millis
```

And then to configure:

```python
> client = SamsaraClient(url="http://my.samsara.server:9000/", sourceId="source identifier")
```


Now you can publish events to samsara!

### Record and Publish an Event

Samsara SDK buffers events and published them to the Ingestion API
periodically. To record an event in the Samsara buffer:


```python
> client.record_event(eventName="user.logged", sourceId="device1", timestamp=current_time_millis())
```

If the `source_id` is not provided then the one set in the configuration will be used.
If the `timestamp` is not provided then it will be used the current system time.
So this mean that you can record a event just as follow:

```python
> client.record_event(eventName="user.logged")
```
Additionally you can provide your own key-value pairs:

```python
> client.record_event(eventName="user.logged", color="blue", level=10)
```

Alternatively, you can publish bulk events immediately to the
Ingestion API using the publish-events function.

```python
> client.publish_events([{"eventName": "user.logged",
                          "timestamp": current_time_millis(),
                          "sourceId": "e6f01efd-04a9-4c18-a210-2806718b6d43"}])
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
Samsara API. `collections.deque` is used for this purpose. The events are
removed from the buffer only if the publish was successful. Newer
events overwrite the oldest events when the buffer reaches its
capacity.

The interval to publish events and the maximum deque size can
also be configured. Note that at this point the configuration cannot
be changed once it is initialised. Any changes will get reflected when
the app restarts.

```python
DEFAULT_CONFIG = {
    # a samsara ingestion api endpoint  "http://samsara.io/"
    # "url": ""       # - REQUIRED

    # the identifier of the source of these events
    # "sourceId": ""  # - OPTIONAL only for record-event

    # whether to start the publishing thread.
    "start_publishing_thread": True,

    # how often should the events being sent to samsara
    # in milliseconds
    # default 30s
    "publish_interval": 30000,

    # max size of the buffer, when buffer is full
    # older events are dropped.
    "max_buffer_size": 10000,

    # minimum number of events to that must be in the buffer
    # before attempting to publish them
    "min_buffer_size": 100,

    # network timeout for send operaitons (in milliseconds)
    # default 30s
    "send_timeout": 30000,

    # whether of not the payload should be compressed
    # allowed values "gzip" "none"
    "compression": "gzip"

    # NOT SUPPORTED
    # add samsara client statistics events
    # this helps you to understand whether the
    # buffer size and publish-intervals are
    # adequately configured.
    # "send_client_stats": True
}
```

## License

Copyright © 2016 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
