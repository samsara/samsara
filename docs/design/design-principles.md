---
layout: page
title: Design principles
subtitle: "Overview of Samsara's Design Principles and internals"
nav: documentation

author:
  name:  Bruno Bonacci
  image: bb.png
---

> _"The price of reliability is the pursuit of the utmost simplicity." (Tony Hoare)_

## Simple.

When I designed this system I had very hard timeline constraint. In
six weeks we went from a POC in my laptop to a scalable,
fault-tolerant production environment with just two people doing
everything: design, development, testing, operations, monitoring and
production support. With such timeline, I had to keep things
very very simple.

The decision was to only use things which were easy to setup, easy to
understand, easy to debug, easy to maintain and which didn't require a
lot of time and effort in accurate tuning.

The choice of the language was easy, most of the tooling picked was on
the JVM, and [Clojure](http://clojure.org) is a very good choice when
the system is primarily data oriented. Its properties of immutable,
functional LISP dialect made it a excellent choice for this project.
Most of all, the ability to do
[REPL Driven Development](http://blog.jayfields.com/2014/01/repl-driven-development.html)
allowed us to cut the development time enormously maintaining fairly
good quality.

Another important aspect was to be able to debug the system easily.
Again the REPL came useful a few times providing the possibility to
connect to a running system and inspect its state. Additionally, the
ability to easily inspect the data at every stage was a paramount
capability.

The real-time processing part was the big question. Loads of
frameworks were available at the time. Things like:
[Storm](http://storm.apache.org/),
[Spark Streaming](http://spark.apache.org/streaming/),
[Samza](http://samza.apache.org/), [Flink](https://flink.apache.org/)
were widely used, [Onyx](http://www.onyxplatform.org/) was a new
player in this field.  Yet, some had complicated processing semantics
to learn, required complicated clusters setup, spend time on tuning
topologies and some of them were mandating specific application
packaging requirements.

We decided to start simple. **The fundamental idea was to encapsulate
the stream processing into a (Clojure) function which was taking in
input a stream and producing a new (richer) stream as output.**

With such approach we could have picked any of the more established
stream processing systems later on and use it as scalable execution
environment as the semantic of stream processing were encapsulated
in our core; but to begin with we wrote our own Kafka consumers.

This choice became fundamentally important later on as it forged the
base for deciding how to scale the system, providing strong guarantees
(ordering) for event processing and high data locality.



## Real-time

Users of Samsara want to be able to see changes in metrics as they
happen.  Providing speed at scale was an important goal. _Samsara
provides "Real Time" Analytics_, but how much of a real time is
depends on various factors.

With this system I'm not aiming to provide milliseconds or microseconds
latency. It is more what normally it is called _near real-time_ and
it ranges from **sub-second to a few seconds** end-to-end latency.
The good thing is that the latency is *tuneable*.

What does it mean to have _tuneable latencies_?

In such systems you can typically trade _latency_ for _throughput_.
Which means that if you accept to have slightly longer latencies,
you can gain faster throughput. This is simply achieved by packing
more events in batches.
However if you need shorter latencies you can configure Samsara
to sacrifice some of the throughput to gain shorter latencies.

In most of real life projects the throughput is not a variable that you
can control, you have a number of users which will send a number of events
and the system has to cope with it. So if _lock_ the throughput dimension,
there is another dimension which can be influenced, and it is the amount
of hardware necessary to deliver a given throughput/latency value.

Ultimately in a cloud environment the amount of hardware used reflect the
infrastructure cost, in such environment you trade latency for cost.


![Tuneable latency](/docs/images/design-principles/tuneable-latency.gif)
