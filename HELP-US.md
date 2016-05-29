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
  * Python
  * PHP
  * Go
  * Ruby
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

## Qanal re-design

_Skills required: Kafka, ElasticSearch, Clojure_

We started to redesign a new implementation of our indexing system
(Qanal).  Qanal takes processed data from Kafka topics and index the
content into ElasticSearch.  The objective of this work is to make
Qanal more robust in face of failures, and elastically scalable (aka:
you just need to add more nodes).

Some of the new design description and implementation are already
on their way and you can see more [here](https://github.com/samsara/samsara/blob/qanal-refactor/qanal/doc/state-machine.md)

However following the recent release of
[Kafka Connectors](http://www.confluent.io/developers/connectors) we
are thinking that probably we could use this new technology and one of
the ready made component to better achieve our goals.

The work to do here is to understand better the capabilities of the
Kafka-ElasticSearch Connector and see if this fits what we are trying
to achieve. In the best option we can replace completely Qanal with a
ready-made connector. In the worst case, we will need to develop
ourselves.

### Core re-design.

_Skills required: Kafka, Kafka-Streams, Clojure_
