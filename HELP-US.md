# Open contribution list.

If you are interested in contributing to Samsara here is a list of
things the project needs which you might help us with.

If you find anything you are interested con contribute or
you wish to talk to us about some other ideas please contact
us to the email address on our [website](http://samsara-analytics.io/about/).

## Clients for other platforms/languages.

_Skills required: languages, multi-threading, lock-free (CAS), http_

We are looking to add client support for the following languages:

  * Java
  * PHP
  * Go
  * Javascript/Web
  * NodeJs
  * C#
  * Java/Android

Clients are probably the easiest way to approach the project.
Although simple on the surface they present challenges in their
multi-threading aspect. All clients should follow the same two-level
apis.  We offer a low level api called `publish-events` which takes in
input a list of events, validates them and perform the HTTP POST to
the ingestion api.

The second level of api, called `record-event`, manages client
state. It buffers the events in a in-memory ring-buffer and using a
background thread publishes the content of the buffer using the
`publish-events` api call.

There are some design goal we are trying to maintain across all client
such as deliver a lock-free CAS based implementation so that low-latency
environments could safely use such type of clients.

Mobiles platforms are slightly more complicated as they have to
guarantee that buffers are persisted across application executions,
and they have to use application specific api to monitor application
state.

## Deployment options and Kubernetes

_Skills required: docker, kubernetes, shell scripting_

Although every component is isolated in a Docker container,
we still need to define and provide solution for a variety
of installation sizes and purposes. From the single machine
for development or demo, to a large scale production environment
where certain component live in their dedicated nodes.

We have built some deployment descriptors for early versions
of Kubernetes, we need now to revisit these and provide a
production ready solution for various sizes.

## Qanal redesign

_Skills required: Kafka, Cloud, ElasticSearch, Clojure, Distributed System design_

We started to redesign a new implementation of our indexing system
(Qanal).  Qanal takes processed data from Kafka topics and index the
content into ElasticSearch.  The objective of this work is to make
Qanal more robust in face of failures, and elastically scalable (aka:
you just need to add more nodes).

Some of the new design description and implementation are already
on their way and you can see more [here](https://github.com/samsara/samsara/blob/master/qanal-refactor/doc/state-machine.md).

Much of the work here will be in common with the redesign of the CORE.
The key decisions for the redesign are:

  - Platform independence: it should be able to to run in the same
    way whether it is running with Kafka, with Kinesis or Azure
    MessageHubs
  - Elastically Scalable: Design a protocol for sharing the topics
    and partitions to process across all available processing resources
  - Fault-tolerant: make sure it is able to recover from any sort
    of issue.

### Core redesign.

See: Qanal redesign

Additional goal for core is to simplify the processing subsystem to use
just simple function and produce less garbage during the processing.

_Skills required: Kafka, Cloud, ElasticSearch, Clojure, Distributed System design_


### Module system design.

The pluggable modules are the real long term benefit of Samsara.
The aim is to design a number of common functions required
by many analytics systems and provide them as built-in modules.
Modules must be loadable dynamically and be configurable.

Modules have to compile a pipeline, and since pipelines may
contains more pipelines modules can compose indefinitely.

When we talk about pipeline here we mean Moebius' pipelines.
From a high level view a module should be seen just a function
which takes a configuration and returns a pipeline, such as:

    (my-module config) -> pipeline

A way to configure modules and load them must be designed.
Initially thought about Components, but now not sure it is
actually the right idea.

_Skills required: Clojure_

### Leiningen template

To simplify development of stream processing create a leiningen
template with a basic skeleton for samsara-core.

_Skills required: Clojure_

### Improving website

We would greatly appreciate help on getting the website look more polished,
dynamic, and fresh like any modern website.

Also on the documentation side there is much need of help with good clear
guides as well as examples.

_Skills required: HTML, CSS, Jekyll_

### Logo

Are you a web designer? Samsara is looking for a (better) logo as well.

_Skills required: Web-design_
