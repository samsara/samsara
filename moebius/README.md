# Möbius (Moebius) [![Circle CI](https://circleci.com/gh/samsara/samsara-moebius/tree/master.svg?style=svg)](https://circleci.com/gh/samsara/samsara-moebius/tree/master) [![Dependencies Status](http://jarkeeper.com/samsara/samsara-moebius/status.png)](http://jarkeeper.com/samsara/samsara-moebius)

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
   {:eventName "game.ad.displayed"    :timestamp 1430760258402 :sourceId "device1"}
   {:eventName "game.level.completed" :timestamp 1430760258403 :sourceId "device1" :levelCompleted 1}
   {:eventName "game.level.completed" :timestamp 1430760258404 :sourceId "device1" :levelCompleted 2}
   {:eventName "game.ad.displayed"    :timestamp 1430760258405 :sourceId "device1"}
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

 ;;=> {:eventName "game.started"
 ;;    :timestamp 1430760258401
 ;;    :sourceId "device1"
 ;;    :level 1
 ;;    :game-name "Apocalypse Now"}
```

Easy enough. Next thing we would like to do is to inject the current
level to all "game.level.completed" events. By doing so we will simplify
queries such as "Average current level over time".

```Clojure
(defenrich current-level
  [{:keys [levelCompleted] :as event}]
  (when-event-name-is event "game.level.completed"
                 (inject-as event :level (inc levelCompleted))))
```

This is another enrichment example as we `enrich` a particular set of
events with additional information. `when-event-name-is` compares the `eventName`
to a given name. `inject-as` it assoc the `:level` property into the event,
as long as the value is not `nil`. Now every "game.level.completed" event
will be enriched with this new property. All others will be left unchanged.
Let's try it out.

```Clojure
(current-level {:eventName "game.level.completed"
                :timestamp 1430760258403
                :sourceId "device1"
                :levelCompleted 1})

;;=> {:eventName "game.level.completed"
;;    :timestamp 1430760258403
;;    :sourceId "device1"
;;    :levelCompleted 1
;;    :level 2}


(current-level {:eventName "game.ad.displayed"
                :timestamp 1430760258402
                :sourceId "device1"})

;;=> nil
```

When applied to a non matching event, the `when`-like clause will
return `nil` and the pipeline processor will interpret this as if you
don't want to change the event. This is just a little simplification
to avoid having to return the original event when you do want to
change it. In case you want to discard the event, then you can use
the `deffilter` macro to define a filter.


### Filtering

Sometimes you want to filter out some of the events you receive. Although
this is not very frequent it might still happen. Usually is better to store
everything as you never know if in the future you will need these events.

So if you want to fiter some events you can create a filter with `deffilter`.
Let's assume that we want to remove all events called "game.ad.displayed":


```Clojure
(deffilter no-ads [{:keys [eventName]}]
  (not= eventName "game.ad.displayed"))


;; no surprise here the predicates work like in filter function
(no-ads {:eventName "game.level.completed"
         :timestamp 1430760258403
         :sourceId "device1"
         :levelCompleted 1})

;;=> true

;; when it doesn't match `false` or `nil` is returned
(no-ads  {:eventName "game.ad.displayed"
          :timestamp 1430760258402
          :sourceId "device1"})
;;=> false
```

### Correlation

Another interesting capability of a stream processing system is to
generate/derive new events from a given event.  The capability to
generate new events is very important in order to keep client small and
send only a minimal number of significant events and do the hard work
on the server side.

In our example let's assume that every time a user starts from the
`level 1` it means that a new user is starting the game. Obviously most
of the time there are better ways to find if there are new users playing
with your new game, but for the sake of this example let's assume that
derive this information in this way it make sense. So let's write a
correlation function.

```Clojure
(defcorrelate new-player
  [{:keys [eventName level timestamp sourceId] :as event}]

  (when (and (= eventName "game.started")
             (= level 1))
    [{:timestamp timestamp :sourceId sourceId :eventName "game.new.player"}]))
```

A correlation function can return `nil`, `[]`, *1 or more events`.
**Every new event generated here will be processed by the same
Moebius pipeline as if was send by the client**.
In this case when the event matches the criteria selected,
an new event is returned. Let's try it into the REPL.

```Clojure
(new-player {:eventName "game.started"
             :timestamp 1430760258401
             :sourceId "device1"
             :level 1})

;;=>[{:timestamp 1430760258401
;;    :sourceId "device1"
;;    :eventName "game.new.player"}]

```

### Compose your processing

Now let's put all the things together into a single streaming function.

```Clojure
;; With the `moebius` function you can combine all the streaming
;; processing functions pretty much in the same way as `comp` does
;; ** with the important difference that the function listed will
;;    be executed in the same order they apper (left-to-right)**

(def mf (moebius
         game-name
         current-level
         no-ads
         new-player))
```

`moebius` return a function which will apply all composed functions to
**all given events**. It takes an initial state as well, however if
all processing functions are stateless, the state will be returned
unchanged. We will explore more about the stateful processing later.


```Clojure
(mf nil events)

;;=>[nil ;; <-- state
;;  [{:eventName "game.started", :game-name "Apocalypse Now", :level 1, :sourceId "device1", :timestamp 1430760258401}
;;   {:game-name "Apocalypse Now", :timestamp 1430760258401, :sourceId "device1", :eventName "game.new.player"}
;;   {:levelCompleted 1, :eventName "game.level.completed", :game-name "Apocalypse Now", :level 2, :sourceId "device1", :timestamp 1430760258403}
;;   {:levelCompleted 2, :eventName "game.level.completed", :game-name "Apocalypse Now", :level 3, :sourceId "device1", :timestamp 1430760258404}
;;   {:levelCompleted 3, :eventName "game.level.completed", :game-name "Apocalypse Now", :level 4, :sourceId "device1", :timestamp 1430760258406}
;;   {:eventName "game.stopped", :game-name "Apocalypse Now", :level 4, :sourceId "device1", :timestamp 1430760258407}]]
```

A few things need to be noted here.

  - The `game-name` has been applied to all events, even the event
    generated by the correlation function.
  - The `current-level` has been injected in all `game.level.completed`
  - The `game.ad.displayed` are now present in the result. The functions
    after `no-ads` wouldn't receive the event at all, while the
    functions which appear before will receive it and they could do some
    processing on it before it get discarded.
  - Finally the `game.new.player` is generated and it went through the
    full processing. Infact the `game-name` has been injected even if
    the function appears before the corellation function.

The function generated by `moebius` can be used to process events in
batches of any size.


## Pattern matching

Sometimes it is easier to express the complicate conditions in terms of
pattern matching. For this purpose we integrate
[core.match](https://github.com/clojure/core.match).  The macro
`when-event-match` allows to match an event based on its
properties. Pattern are processed in order of appearance so if you have
multiple pattern matching the first one appearing in the list is going
to be matched and the associated expression will be executed.
If nothing matches, then the event is return unchanged.
To support this an implicit `:else` statement is included in the match,
so you can't use it in your match expressions.


```Clojure
(let [event {:eventName "game.started" :level 8}]
   (when-event-match event
     [{:eventName "game.started" :level 0}]               (assoc event :new-player true)
     [{:eventName _ :level (_ :guard even?)}]             (assoc event :start :even-level)
     [{:eventName _ :level (_ :guard #(= 0 (mod % 11)))}] (assoc event :level-type :extra-challenge)
     [{:eventName "game.new.level" :level _}]             (assoc event :level-type :normal)))

```
In this case the event `{:eventName "game.started" :level 8}` is going to match
the second expression.


## Glob matching

The `eventName` can be any string, however we recommend you to use a
series of words separated by dots, such as:
`<segment>.<segment>.<...>.<segment>`.  By doing so you will be able to
leverage the glob matching facilities provided by the framework.

The glob matching works by matching `*` _to any single segment_ and
matching `**` to _multiple segments_.

Here some example of glob matching.

```Clojure
(match-glob "game.*.started"  "game.level.started")   ;;=> truthy
(match-glob "game.*.started"  "game.level.2.started") ;;=> false
(match-glob "game.**.started" "game.level.2.started") ;;=> truthy
(match-glob "game.**"         "game.level.5.stopped") ;;=> truthy
(match-glob "game.**"         "game.anything.else")   ;;=> truthy
(match-glob "game.**.ended"   "game.1.2.3.ended")     ;;=> truthy
```

To use you simply use in a condition statement:

```Clojure
(defenrich current-level
  [{:keys [levelCompleted eventName] :as event}]
  (when (match-glob "game.**.completed" eventName)
     (inject-as event :level (inc levelCompleted))))
```


## License

Copyright © 2015 Samsara's authors

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
