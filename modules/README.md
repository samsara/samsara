# samsara-bultin-modules

This projects contains Samsara's built-in modules.

## Available modules

  * Session management
    * [Auto detect session duration from start/stop events](#session-boundaries-correlation).


### Session boundaries correlation

This modules enables you to detect events which mark the beginning and the end
of an activity and automatically compute the duration of the session.

For example if you have two events as follow:

```Clojure
(def events
  [{:timestamp 1436625946940
     :eventName "game.play.started"
    :sourceId  "device1"}

   {:timestamp 1436626254608
    :eventName "game.play.stopped"
    :sourceId  "device1"}])
```

Given these two simple events this module can find the duration of the game session.

```Clojure
(require '[moebius.core :refer :all]
          [moebius.kv :as kv])
(def processor (moebius session-boundaries-correlation))
(def state (kv/make-in-memory-kvstore))
(processor state events)
```

This will produce a new event which contains the `duration` in milliseconds
between the two events.

```Clojure
[{:eventName "game.play.started", :sourceId "device1", :timestamp 1436625946940}
  {:eventName "game.play.stopped", :sourceId "device1", :timestamp 1436626254608}
  {:stopTs 1436626254608,
   :eventName "game.play.done",
   :inferred true,
   :duration 307668,
   :startTs 1436625946940,
   :sourceId "device1",
   :timestamp 1436625946940}]
```

This is very useful to answer question like:
  * *What is the average game play?*
  * *How much is the total time of game played by day or by hour?*
  * *Which are the top 10 devices who played the most?*
  * ... and many others ...

For more info check: [Session boundaries](/doc/session-boundaries-module.md)

## License

Copyright © 2015 Samsara's authors

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
