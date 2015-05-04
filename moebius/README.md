# Möbius (Moebius)

A system to process and enrich and correlate events in realtime.


![Moebius strip](/doc/images/Moebius_strip.png)


## Usage

Moebius contains a set of function which helps you to perform
tranformations on a stream of events.  Moebius processes Samsara's
events which are defined as follow.

```Clojure
{:timestamp 1430760258405           ; milliseconds from EPOC
 :eventName "any.meaningful.name"   ; a descriptive name for an event (typically dotted)
 :sourceId  "any-identifier"}       ; an identifier of the who is sending the event (such as: userId, deviceId, clientId etc)
```

This is the minimal event which could be sent to Samsara. However you
can speficfy additional properties as part of the event such as:

```Clojure
{:timestamp   1430760258405
 :eventName   "any.meaningful.name"
 :sourceId    "any-identifier"
 
 ;; it supports any additional k/v pairs
 :color       "red"
 :temperature 45 }
```

How Moebius works is better explained with an example.

Let's assume we have an imaginary game called "Apocalypse now" which sends
to Samsara the following events:

```Clojure
(def events
  [{:eventName "game.started"         :timestamp 1430760258401 :sourceId "device1" :level 1}
   {:eventName "game.ad.shown"        :timestamp 1430760258402 :sourceId "device1"}
   {:eventName "game.level.completed" :timestamp 1430760258403 :sourceId "device1" :levelCompleted 1}
   {:eventName "game.level.completed" :timestamp 1430760258404 :sourceId "device1" :levelCompleted 2}
   {:eventName "game.ad.shown"        :timestamp 1430760258405 :sourceId "device1"}
   {:eventName "game.level.completed" :timestamp 1430760258406 :sourceId "device1" :levelCompleted 3}
   {:eventName "game.stopped"         :timestamp 1430760258407 :sourceId "device1" :level 4}])

```

### Enrichment

Let's say that in our server we manage multiple game, and we want to be able to distinguish
the events coming from this game from those of other games. To do so we can `enrich` every
incoming event from a specific endpoint with an attribute `:game-name`.
To do so we use an enrichment function which takes an event as argument and injects the game name.

```Clojure
(defenrich game-name
  [event]
  (assoc event :game-name "Apocalypse Now"))
```

This function will add the `:game-name` attribute with the value
"Apocalypse Now" to every event in the stream. Let's test it out:

```Clojure
(game-name {:eventName "game.started" 
            :timestamp 1430760258401 
            :sourceId "device1" 
            :level 1})
            
 ;;=> [{:eventName "game.started" 
 ;;     :timestamp 1430760258401 
 ;;     :sourceId "device1" 
 ;;     :level 1 
 ;;     :game-name "Apocalypse Now"}]
```

Easy enough. Next thing we would like to do is to inject the current
level to all "game.level.completed" events. By doing so we will simplify
queries such as "Average current level over time".

```Clojure
(defenrich current-level
  [{:keys [levelCompleted] :as event}]
  (when-event-is event "game.level.completed"
                 (inject-as event :level (inc levelCompleted))))
```

This is another enrichment example as we `enrich` a particular set of
events with additional information. `when-event-is` compares the `eventName`
to a given name. `inject-as` it assoc the `:level` property into the event,
as long as the value is not `nil`. Now every "game.level.completed" event
will be enriched with this new property. All others will be left unchanged.
Let's try it out.

```Clojure
(current-level {:eventName "game.level.completed" 
                :timestamp 1430760258403 
                :sourceId "device1" 
                :levelCompleted 1})

;;=> [{:eventName "game.level.completed" 
;;     :timestamp 1430760258403 
;;     :sourceId "device1" 
;;     :levelCompleted 1
;;     :level 2}]


;; when applied to a non matching event, the event is left unchanged

(current-level {:eventName "game.ad.shown"
                :timestamp 1430760258402 
                :sourceId "device1"})

;;=>[{:eventName "game.ad.shown"
;;    :timestamp 1430760258402
;;    :sourceId "device1"}]


```



## License

Copyright © 2015 Samsara's authors

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
