---
layout: page
title: Client libraries general design
subtitle: "The design principles which should be followed by all clients libraries."
nav: documentation

author:
  name: Bruno Bonacci
  image: bb.png
---

The general design of all clients is based on a two levels
API. Although the aim is to make idiomatic clients for every language
we want to keep an high level design common across all clients, given
that the language primitives offers the required capabilities.

![Clients design](/docs/images/clients-design.jpeg)<br/>
_**[~] Clients design.**_

## Lower level API (`publish-events`) The first lower level it is just

a wrapper on top of the communication protocol with the
**ingestion-api** (HTTP POST). See the
[ingestion-api spec](/ingestion-api/spec/ingestion-api-spec.yaml) for
more info.

The signature looks like: `publish-events(URL, List<Events>)`.
At this stage the `publish-events` does the following three things:

   - Validate events (check the existence and type of the three
     required keys for each event: **sourceId**, **timestamp** and
     **eventName**)
   - create a JSON payload with all events
   - optionally compress the payload (**gzip**)
   - add the `X-Samsara-publishedTimestamp` HTTP header
   - and finally performs the HTTP POST to the given endpoint URL

## Higher level API (`record-event`) The second level is the buffering

layer. For clients which wish to buffer locally the events before
posting them to the ingestion-api we want to provide another API.
This api is called `record-event(Event)` and it takes a single event.
The job of this API is to validate the single event and add it to a
in-memory
[ring-buffer](https://en.wikipedia.org/wiki/Circular_buffer).
Periodically (configurable) a background thread takes the content of
the ring buffer and publishes the events using the `publish-events`
api.  The buffer can have a configurable size and if the buffer is
full we currently implement only one strategy which is to drop the
oldest events. In future we might add more strategies.  The major
concern of this api is to provide a fast and thread-safe way to record
the events in the buffer.  This API has to have good performance
characteristics and use
[CAS](https://en.wikipedia.org/wiki/Compare-and-swap) where possible
(given that the language primitives allows it). We discourage the use
of [mutex](https://en.wikipedia.org/wiki/Mutual_exclusion) as they can
be an unnecessary burden for the system and cause excessive context
switches. Contrarily CAS can run on user-space whereas mutex are
coordinated by the kernel.

Once the background thread successfully publishes the events to the
ingestion-api, it discards them from the ring-buffer. Clients must be
careful to only discards events which have been sent. The HTTP POST
might takes several hundreds of milliseconds up to a few seconds,
during this time another thread might have recorded additional events
in the buffer. Therefore is crucial to keep track of which event were
sent and discard only these from the buffer.

The goal is to provide a common api for all languages where one can
trust the behaviour of these two APIs. Developers of client libraries
have to try to follow these guidelines and make clients as much
idiomatic as possible for each language.
