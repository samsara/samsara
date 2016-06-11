---
layout: page
title: Design principles
subtitle: "Overview of Samsara's Design Principles and internals"
nav: documentation

author:
  name:  Bruno Bonacci
  image: bb.png
---

Table of contents:

  * [Simple](#simple)
  * [Real-time](#realtime)
  * [Aggregation "on ingestion" vs "on query"](#aggregation)
    * [Aggregation "on ingestion"](#agg_ingestion)
    * [Aggregation "on query"](#agg_query)

---

## <a name="simple"/> Simple.

> _"The price of reliability is the pursuit of the utmost simplicity." (Tony Hoare)_

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
capability. Rushing the implementation so quickly would cause
possible defects which could be anywhere in the system, so to try
to be _human-fault tolerant_ was another property we tried to
bake in the original design. Storing the raw data in verbatim
in deep storage, ability to safely re-process the data, possibility
to tear down one part of the system without losing events have
been factored in the design from the beginning.

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


---

## <a name="realtime"/> Real-time.

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


![Tuneable latency](/docs/images/design-principles/tuneable-latency.gif)<br/>
_**[*] You can trade latency for throughput or cheaper hardware..**_


---

## <a name="aggregation"/> Aggregation "on ingestion" vs "on query".

The design of analytics system broadly divides in two large
categories: _the ones who aggregate data during the ingestion_ and
_the ones who aggregate data at query time_. Both approaches have
advantages and drawbacks, this is meant to be a critique to any of
these systems. We will try to see how these systems work and what are
the key differences to better understand why picked a particular
solution with Samsara.

### <a name="agg_ingestion"/> Aggregation on ingestion.

The following image depicts what commonly happens in system which perform
aggregation on ingestion.

![Aggregation on ingestion](/docs/images/design-principles/agg-on-ingestion.gif)<br/>
_**[*] Upon ingestion of e new event, counters are updated in memory.**_

On the left side we have the _time_ flowing from top to bottom with a
number of different events sent to the system. The strategy here is to
split the _time continuum_ in discrete buckets. The size of the
buckets depends on your system requirement. These buckets are often
called _windows_ as well.

Let's assume we want to know the average number of events over the
last 3 seconds.

For this we need to create windows of _1 second_ each in our ingestion
layer.  Every bucket has a timestamp associated so that when we
receive a event we can look at the event's timestamp and increment the
counter for that bucket.

As the time flow, we will receive a number of events and increment the
buckets which typically are store in memory for performances reasons.
After a certain period of time we will have to flush these in-memory
buckets back to a storage layer to ensure durability and free up some
memory for new buckets.

With this pretty simple approach there is already a number of
difficult challenges to address. For instance how often will we flush
the buckets to the durable storage? If we do it too often we will
overwhelm the storage with loads of very little operations.  On the
other side, if we decide to keep in memory for too long we incur the
risk for running out of memory or loosing a number of events due to a
process crash.

Additionally, how do we manage events which arrive late? If they
arrive while we still have the corresponding bucket in memory, we can
simply increment the corresponding counter. However, if we have
already flushed the bucket to the durable store and we have freed up
the memory we will be left with no corresponding bucket in memory.

There are several strategies to deal with late arrivals and there are
several papers which explain benefits of the different strategies.
Whatever decision we take these are hard problem to solve in a reliable
and efficient manner.

Once data is flushed to the permanent store is now ready to be queried.
Some system allow to query also the in-memory buckets at the ingestion
layer to shorten the latencies.
The query engine will have to select a number of buckets based on
the query parameters and sum all counters. At this point is ready
to return the result back to the user.

If you think that this was complicated just to get the average number
of events, just sit tight, there is more to come.

Now that I've got the overall number of events I realize that I need
to understand how this value breaks down in relation to the type
(shape) of events I've received.

To do so, one bucket per second is not enough. _We need a bucket
per second per type of event_. With such buckets structure
I can now compute not only the total number of events per second,
but also the number of _stars_ per seconds, for example.

There is more. What if, what I really wanted was the number of _red
stars_ per second. Well, we have to start from scratch again, this
information is now available in the current information model because
we took the raw events, we aggregated them and discarded the original
events in favour of the aggregated view only.

In order to compute the number of _red stars_ per second we need to go
back to our information model and duplicate all buckets for every type
of colour we handle.

#### Number of buckets explosion.

The image below shows the difference between the three queries on the
same events and the buckets required to compute them.

![Num Buckets explosion](/docs/images/design-principles/agg-explosion.gif)<br/>
_**[*] The number of buckets explodes for every new dimension to explore**_

You can easily see how, even in this very simple example, the number
of buckets and the complexity start to explode exponentially for
every dimension that we need to track.

For example just for this simple example, if we want to be able to run
query across this data and retain 1 year worth data we will need to
create a bucket for every second in a year (31M circa), however
querying across several weeks or months can become prohibitive. A
common strategy is to roll small buckets up into larger one. I could
aggregate buckets at second granularity into minutes, minutes into
hours, hours into days etc. By doing so, if I'm requesting the number
of events across the last 3 days I can just aggregate last 3 daily
buckets, or mix daily buckets with hours, minutes and seconds to get
finer granularity. So if I need to query across last 3 months I have a
significant smaller number of buckets to lookup.

However this has a cost. In this picture there is the breakdown of
how many buckets will be required to be able to flexibly and efficiently
query the above simple example.


![Num Buckets](/docs/images/design-principles/num-buckets.gif)<br/>
_**[*] The number of buckets requires even for simple cases is huge**_


As you can see we need to keep track of *32 million* buckets just for
1 year worth of data, and this _just for the time buckets_, now we
have to multiply this figure for every dimension in your dataset and
every cardinality in each and every dimension.  For this basic example
which only contains 2 dimensions: the event type (4 ctegories) and the
colour (2 categories) we will require **over 256 million** buckets.

Luckily there is another way.


### <a name="agg_query"/> Aggregation on query.
