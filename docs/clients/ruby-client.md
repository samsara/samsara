---
layout: page
title: Samsara Ruby Client
subtitle: Ruby Client for Samsara.
nav: documentation
tab_bar: clients
---

## Usage

To use the Samsara SDK, install gem 'samsara_sdk'

```bash
$ gem install samsara_sdk
```

To bring it into the REPL:

```ruby
> require 'samsara_sdk'
```

And then to create client instance:

```ruby
> client = SamsaraSDK::Client.new(url: 'http://my.samsara.server:9000/', sourceId: 'source identifier')
```
It creates a Samsara Client instance. Now you can publish events to Samsara!
Note, that URL is a required configuration option and must be provided.
If any of the configuration options is invalid `SamsaraSDK::ConfigValidationError` will be raised.

### Record and Publish an Event

Samsara SDK buffers events and publishes them to the Ingestion API
periodically. To record an event in the Samsara buffer:


```ruby
> client.record_event(eventName: 'user.logged', sourceId: 'device1', timestamp: 1_479_988_864_057)
```

If the `sourceId` is not provided then the one set in the configuration will be used.
If the `timestamp` is not provided then client automatically generate current system time in milliseconds.
This means that you can record event just by:

```ruby
> client.record_event(eventName: 'user.logged')
```
Additionally you can provide your own key-value pairs:

```ruby
> client.record_event(eventName: 'user.logged', color: 'blue', level: 10)
```

`record_event` stores events in a thread-safe buffer so you can use it in several threads, if needed.

Alternatively, you can publish bulk of events immediately to the
Ingestion API using the publish-events method.

```ruby
> client.publish_events([{eventName: 'user.logged',
                          timestamp: 1_479_988_864_057,
                          sourceId: 'e6f01efd-04a9-4c18-a210-2806718b6d43'}])
```

The events will be sent to Samsara immediately.
Note that `publish_events` method signature is Array<Hash> so if you want to send only one event
it should be wrapped in array as well.

Also please note that `record_event` and `publish_events` can raise `SamsaraSDK::EventValidationError`
if any of the given events doesn't conform to Event specification.

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
Samsara API. `circular buffer` implementation is used for this purpose. The events are
removed from the buffer only if the publish was successful. Newer
events overwrite the oldest events when the buffer reaches its
capacity.

The interval for events publishing and the maximum buffer size can
be configured as well. Note that configuration can not
be changed once the client has been initialised. Any changes will get reflected when
the client is restarted.

```ruby
    @defaults = {
      # Samsara ingestion api endpoint "http://samsara-ingestion.local/"
      url: '',

      # Identifier of the source of these events
      # OPTIONAL used only for record-event
      sourceId: '',

      # Start the publishing thread?
      start_publishing_thread: TRUE,

      # How often should the events being sent to Samsara
      # in milliseconds.
      # default = 30s
      publish_interval_ms: 30_000,

      # Max size of the buffer.
      # When buffer is full older events are dropped.
      max_buffer_size: 10_000,

      # Minimum number of events that must be in the buffer
      # before attempting to publish them.
      min_buffer_size: 100,

      # Network timeout for send operations
      # in milliseconds.
      # default 30s
      send_timeout_ms: 30_000,

      # Should the payload be compressed?
      # allowed values :gzip, :none
      compression: :gzip,

      # NOT CURRENTLY SUPPORTED
      # Add Samsara client statistics events
      # this helps you to understand whether the
      # buffer size and publish-intervals are
      # adequately configured.
      # send_client_stats: TRUE
    }
```

## License

Copyright © 2016 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
