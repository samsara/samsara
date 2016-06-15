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

  * [Simple.](#simple)
  * [Real-time.](#realtime)
  * [Aggregation "on ingestion" vs "on query".](#aggregation)
    * [Aggregation "on ingestion".](#agg_ingestion)
    * [Aggregation "on query".](#agg_query)
    * [Which approach is best?](#agg_best)
  * [Samsara's design overview.](#overview)
    * [Cloud native vs Cloud independent](#cloud)
    * [Cutting some slack.](#slack)

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
_**[o] You can trade latency for throughput or less hardware..**_


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
_**[o] Upon ingestion of e new event, counters are updated in memory.**_

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
_**[o] The number of buckets explodes for every new dimension to explore**_

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
_**[o] The number of buckets required even for simple cases is huge**_

As you can see we need to keep track of *32 million* buckets just for
1 year worth of data, and this _just for the time buckets_, now we
have to multiply this figure for every dimension in your dataset and
every cardinality in each and every dimension.  For this basic example
which only contains 2 dimensions: the event type (4 categories) and the
colour (2 categories) we will require **over 256 million** buckets,
and above all __we still haven't store the event itself__.

Luckily there is another way.


### <a name="agg_query"/> Aggregation on query.

If we consider the same situation with a typical aggregation on query
architecture we see a different story.

Again on the left side we have time flowing from top to bottom, and a
number of events. This time we consider the _time continuum_ as fluid
as the reality, no need to create discrete buckets.  As we receive an
event, at the ingestion we can process it straightaway.  While in the
previous scenario we have to be mindful of the number of dimensions,
here we are encourage the enrichment of each events with many more
dimensions and attributes which makes the event easier to query.

Each event is processed by a custom pipeline in which enrichment,
correlation and occasionally filtering, take place. At the end of the
processing pipeline we have a richer event with more dimensions
possibly denormalised as we might have used some internal dataset to
join to this stream. The output goes into the storage and indexing
system where the organisation is radially different.

![Aggregation on query](/docs/images/design-principles/agg-on-query.gif)<br/>
_**[o] Upon ingestion of e new event, we enrich the event and store into information retrieval inverted index.**_

First major difference is that the indexing system stores the event
itself.  Then it analyses all its dimensions and create a bucket for
every property.  In this bucket it add a pointer back to the original
event.

As the picture show the bucket for the color "red" is pointing to all
red events in our stream, and as more red events arrive are added to
this bucket. Same thing happen for the "yellow" events and for the
different shapes. Virtually, also the time can be considered in a
similar way, although the time is most system is considered a
"special" dimension and it is used for physical partitioning.
But for the purpose of this description we can consider pretty much
in the same way.

These newly create buckets are just
[sets](https://en.wikipedia.org/wiki/Set_(mathematics)) and every set
just contains the ids of the events which have that property.  Now if
we are looking for all events in a specific timeframe, all we need to
do is to take all these sets (buckets) with a timestamp within the
range and to a
[set union](https://en.wikipedia.org/wiki/Set_(mathematics)#Unions).

While if we want to know all "red" events for the same timeframe, we
do a union of the sets related to the time and an
[intersection](https://en.wikipedia.org/wiki/Set_(mathematics)#Intersections)
of the resulting set with the "reds" set.
There are plenty of optimisations to this but the idea stays the same.
This type of index structure is called [Inverted index](https://en.wikipedia.org/wiki/Inverted_index).

Because the aggregation happens at query time, the handling of events
which arrives late is no different than the normal events.  They get
processed in the same way, enriched and stored like any other event.
As they are added to the corresponding set for each dimension they are
directly available for query in their correct position.

### <a name="agg_best"/> Which approach is best?

Both approaches have advantages and drawbacks. Which one you should
choose really depends on the use you are going to do of the data.

The aggregation on ingestion offers good performances even for very
large datasets as you don't store the raw data-points but only
aggregated values. However a problematic point is that you have to
know all the queries you will run ahead of time. So if you have a
_small fix number of queries_ which you have to serve continuously
then that's a optimal approach.

In the projects I've been working on, I was looking for a system which
would be flexible and support interactive and fast data exploration.
In other words you couldn't predict ahead of time all or most of the
queries which would be performed later. In such cases aggregation on
ingestion is the wrong approach. The combinatorial explosion of
dimensions and values would make the ingestion too slow and
complicated. To perform arbitrary queries on any of the available
dimensions you need to store the original value and prepare your
storage for such case.

If query flexibility is what you are looking for, like the ability to
slice and dice your dataset using any of the available dimensions then
the aggregation on query is the only solution.

---

## <a name="overview"/> Samsara's design overview.

In Samsara we focus on agility and flexibility. Even with datasets of
several billion events we can achieve very good performances and
maintain the interactive query experience (most of the queries last
less than 10 seconds).

Let's see how did we managed to implement the "aggregation on query"
approach with large scale datasets.

![High level design](/docs/images/design-principles/high-level-design.jpeg)<br/>
_**[o] Samsara's high-level design.**_

On the left side of the diagram we have the clients which might be
mobile clients, your services or websites, or internet websites
pushing the data to a RESTful endpoint.

The Ingestion-API will acquire the data and immediately store into
Kafka topics. The **Kafka** cluster will replicate the records in
other machines in the cluster for fault-tolerance and durability.

Additionally a process will be continuously listening to the Kafka
topic and as the stream arrives will push the data into a deep
storage.  This can be something cloud based such as **Amazon S3** or
**Azure Storage**, a NAS/SAN in your datacenter or a **HDFS** cluster.
The idea is to store the raw stream so that no matter what happen to
the cluster and the processing infrastructure you will always be able
to retrieve the original data and reprocess it.

The next step is where the enrichment and correlation of the data
happen.  Samsara CORE is a streaming library which allows you to
define your data enrichment pipelines in terms of very simple
functions and streams composition. Stream processing shouldn't be a
specialist skill, but any developer which is mindful of performance
should be able to develop highly scalable processing pipelines.  We
aim to build the full processing pipeline out of plain Clojure
functions, so that it is not necessary to learn a new programming
paradigm but just use the language knowledge you already have.

For those who don't know the Clojure programming language, we are
considering to write language specific bindings with the same
semantics.

Implementing stateless stream processing is easy. But most of real
world application need some sort of _stateful stream processing_.
Unfortunately many stream processing frameworks have little or nothing
to support the correctness and efficiency of state management for
streams processing. Samsara's leverages the amazing
[Clojure's persistent data structures](http://hypirion.com/musings/understanding-persistent-vector-pt-1)
to provide a very efficient stateful processing. The _in-memory_ state
is then backed by a Kafka topic or _spilled_ into a external key/value
store such as **DynamoDB**, **Cassandra** etc.

The processing pipeline takes one or more stream as input and produces
a richer stream as output. The output stream is stored in Kafka as
well.  Another component (`Qanal`) consumes the output streams and
index the data into ElasticSearch. In the same way we store the raw
data we can decide to store the processed data as well in the deep
storage.

Once the data is in **ElasticSearch** it is now ready to be queried.
The powerful query engine of **ElasticSearch** is based on Lucene
which manages the inverted indexes. **ElasticSearch** implemented a
large number of queries and aggregations and it makes simple even very
sophisticated aggregations.

The following picture shows the list of aggregations queries which
**ElasticSearch** implemented (v1.7.x) and more will come.

![ElasticSearch available aggregations](/docs/images/design-principles/els-aggregations.gif)<br/>
_**[o] ElasticSearch available aggregations (v1.7.x).**_

Once the data is into **ElasticSearch** you get all the benefits of a
robust and mature product and the speed of the underlying Lucene
indexing system via a REST API. Additionally, out of the box, you can
visualize your data using **Kibana**, creating _ad-hoc_
visualizations, dashboards which work for both: real-time data and
historical data as both are in the same repository.

![Kibana visualizations](/docs/images/design-principles/kibana-visualizations.jpeg)<br/>
_**[o] Kibana visualizations (from the web).**_

Kibana might not be a best visualization tool out there but is a quite
good solution for providing compelling dashboards with very little
effort.

### <a name="cloud"/> Cloud native vs Cloud independent.

The discussion around cloud native architectures with their benefits
versus the drawbacks is always a hot topic while designing a new
system.  Advocates from both sides have good reasons and valid
arguments.  Cloud native is usually easier to operate and scale, but
with the risk of vendor lock-in. Cloud independent has to rely only on
basic infrastructure when running on a cloud (IAAS) and not use any of
the proprietary PAAS services.  With Samsara we went a step
further. We composed the system with tools which have a cloud PAAS
counterpart.

**Kafka**, **Amazon Kinesis** and **Azure EventHubs** try to solve the
same problem.  Similarly **Cassandra**, **Riak**, **DynamoDB**,
**Azure Table Storage** are again very similar from an high level
view. So while designing Samsara we have take care of not using
functionalities which were not available in other platforms or which
couldn't be implemented in some other way.

The result is that the system can preserve some guarantees across the
different clouds and even provide the same functionality when running
in your own data-center.

The next picture shows some of the interchangeable parts in Samsara's
design.


![Cloud alternatives](/docs/images/design-principles/cloud-support.jpeg)<br/>
_**[o] Green parts are available as of 0.5, the other ones soon to come.**_


### <a name="slack"/> Cutting some slack.

One design principle I was very keen to observe was build around every
layer some fail-safe. In this case I'm talking about the
fault-tolerance against machine failures, but specifically fail-safe
against **human failures**.  If you have ever worked in any larger
project you know that failures introduced by humans mistakes are by
large the most frequent type of failure.  The idea of adding some sort
of resilience against human mistakes is not new.  The old and good
database backup is probably one of the earliest attempt to deal with
this issue. For many years, while designing new systems, I tried to
include some sort of fail-safe here and there, but I wasn't aware of a
appropriate name for it so when people were asking why, my short
answer was something like: _"... because, you know; you never know what
can go wrong"_. It wasn't until 2013 when I watched
[Nathan Marz talk on "Human Fault-Tolerance"](https://www.youtube.com/watch?v=Ipjrhue5bXs)
which it came clear to me what was the pattern to follow.  In the same
way we try to take into account for machine failures, network
failures, power failures etc in our design we should also account for
human failures.

In the talk, Nathan illustrates principles which, if applied, could be
treated as general pattern for human fault-tolerance.  The general
idea is: **if you are building a data system, and you _build upon
immutable facts_, then storing the raw data, you can always go back
and rebuild the entire system based on the raw, immutable facts plus
the corrected algorithms**. This really resonated with what I was
trying to do.

Samsara encourages to publish small essential facts. By definition
they are always "true".  Samsara stores them in durable storage so
that _no matter what happen later, you always have the possibility to
go back and reprocess all the data_.

Not only this but we care about making easy to rebuild a consistent
view of the world, so Samsara's give you support for publishing
external dataset as streams of event as well. Finally we avoid
duplicates in the indexes which could be caused by re-processing the
data by assigning repeatable IDs to every event, at the cost of some
indexing performance. This is a deliberate choice, and it has is
foundation in the concept of _human fault-tolerance_.

There are many other little changes have been made to support this
idea I hope you will agree with me that the system is overall better
with these changes.

---

## Kafka.


## Samsara processing CORE.
