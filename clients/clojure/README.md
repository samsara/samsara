# samsara-clj-sdk

Clojure SDK for Samsara. [![Circle CI](https://circleci.com/gh/samsara/samsara-clj-sdk/tree/master.svg?style=svg)](https://circleci.com/gh/samsara/samsara-clj-sdk/tree/master)

## Usage

To use the Samasara SDK you need to add the following dependency to your `project.clj` file.

```clojure
[samsara/samsara-client "0.1.0-beta"]
```
Load the namespace in the REPL
```clojure
(use 'samsara.client)
```
or as part of your namespace
```clojure
(ns my.project
	(:require [samsara.client :as :samsara]))
```
Now you can start to publish events to samsara.

### Publish an Event
Samsara SDK buffers events and publishes them to Ingestion API periodically. To record an event in the Samsara buffer do
```clojure
(samsara/record-event {:eventName "UserLoggedIn"})
```
Alternatively, you can publish bulk events immediately to the Ingestion API using the publish-events function.
```clojure
(samsara/publish-events [{:eventName "UserLoggedIn"})]
```
The event will be sent to samsara immediately. 

### Event Headers
It is possible to set 'Event Headers' which will be added to all events.
```clojure
(samsara/set-event-headers! {:version "2.2"})
```

### Client ID
Samsara SDK will generate a unique identifier for each client. This property will be added to every event in the "sourceId" field. 

In later versions, the SDK will preserve Client ID during restarts (TODO).


### Advanced configuration

Samsara SDK buffers events and peridically flushes the events to the Samsara API. A ring buffer is used for this purpose. The events are removed from the buffer only if the publish was successful. Newer events overwrite the oldest events when the buffer reaches its capacity.

The interval to publish events and the maximum ring buffer size can also be configured. Note that at this point the configuration cannot be changed once it is initialised. Any changes will get reflected when the app restarts.

```clojure
;; Config
;; Map containing the following config:
;; :url - Samsara URL
;; :publish-interval - How often should events be flushed to the api in seconds.
;; :max-buffer-size - Max size of the ring buffer.
(samsara/set-config! {:publish-interval 60 :max-buffer-size 10000})
```


## License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)

