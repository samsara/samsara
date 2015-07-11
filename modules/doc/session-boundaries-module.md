# Session boundaries correlation module

This modules enables you to detect events which mark the beginning and the end
of an activity and automatically compute the duration of the session.

Common examples are the **start**/**stop** of a game play or when the an app
**start**/**stop** downloading a large payload, or anything which gets
**plugged**/**unplugged**, **open**/**closed**, **connected**/**disconnected**,
**enabled**/**disabled** or **activated**/**deactivated**. Any sort of logical
pair can be used to compute the duration of that particular session.

Pairs (or session gates) can be configured with what make sense for your case.
By default we detect only events whose name terminates by **.started**/**.stopped**,
however it is easy to configure the module to look for other gates name.

Once a closing gate event has been found the system attempt to match it with
the corresponding start if present, in which case a new event will be produced
which is contains both originating events merged into one, with the addition
of a `duration` in milliseconds.

It is typically very simple to add **started**/**stopped** events in the client
app because are typically connected to well defined actions or callbacks,
and don't require to add a local state in the client to compute client side
the duration. Instead we prefer keep the clients events as simple as possible
and make the heavy lifting on the server side.

This is very useful to answer question like:
  * *What is the average game play?*
  * *How much is the total time of game played by day or by hour?*
  * *Which are the top 10 devices who played the most?*
  * ... and many others ...

To make use of this module just add the following function in your `pipeline`
`samsara-builtin-modules.session_boundaries`/`session-boundaries-correlation`.

## Example

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
          [moebius.kv :as kv]
          [samsara-builtin-modules.session_boundaries
            :refer session-boundaries-correlation])

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
   :duration 307668,       ;; <- computed duration
   :startTs 1436625946940,
   :sourceId "device1",
   :timestamp 1436625946940}]
```

## Configuration

You can configure which gates are processed in your stream with the following var.

```Clojure
(alter-var-root
  #'samsara-builtin-modules.session_boundaries/*session-boundaries*
  (constantly
    [{:start-gate (partial match-glob "**.started")
      :stop-gate  (partial match-glob "**.stopped")
      :start-gate-name #(s/replace % #"\.stopped$" ".started")
      :merge-with merge-session-events
      :name-fn    #(s/replace % #"\.started$" ".done")}]

    [{:start-gate (partial match-glob "**.enabled")
      :stop-gate  (partial match-glob "**.disabled")
      :start-gate-name #(s/replace % #"\.disabled$" ".disabled")
      :merge-with merge-session-events
      :name-fn    #(s/replace % #"\.enabled$" ".done")}]))
```

Where:
  * `:start-gate` and `:stop-gate` are function which given the
    `eventName` return truthy if the event is either a start or stop
    of a session.
  * `:start-gate-name` is a function which given a stop gate eventName
    can return how the start event was it called.
  * `:merge-with` is a merging function which will take 3 parameters
    `[propsed-name event-start event-stop]` and return a new event.
  * `:name-fn` which given a start event name returns a new name for the
    combined event.

## Usage

In order to use this module in your streams just add it to the pipeline.

```Clojure
(require '[samsara-builtin-modules.session_boundaries
            :refer [session-boundaries-correlation]])

(pipeline
  ;; your pipeline functions
  session-boundaries-correlation
  ;; more pipeline functions.
  )
```
