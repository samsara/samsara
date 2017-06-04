---
layout: page
title: Samsara Clients
subtitle: Start publishing to Samsara in no time.
nav: documentation
tab_bar: clients
---

## Clients
Samsara provides clients for various languages and platforms. This
will help you start publishing events to Samsara in no time. The
features clients offer are listed below.

If you want to find more about the clients you can read
the [clients common design principles](/docs/design/clients-design.md)
document.

## Features
* Handle connectivity to the API.
* Provide API to publish events in bulk immediately.
* Provide API to buffer events locally and periodically flush to the API.
* Validate the event before publishing.
* Enrich events with default system/device parameters.
* Mobile clients enrich events with device and location information automatically.


## Feature Matrix

| Client     | Available   | Bulk Publish | Buffer events | Compression | Device Info | Location |
|------------|-------------|--------------|---------------|-------------|-------------|----------|
| Clojure    | Y           | Y            | Y             | GZIP        | N           | N        |
| Python3    | Y           | Y            | Y             | GZIP        | N           | N        |
| Ruby       | Y           | Y            | Y             | GZIP        | N           | N        |
| iOS        | Y           | Y            | Y             | N           | Y           | Y        |
| Go         | Y           | Y            | Y             | GZIP        | N           | N        |
| Logger     | Y           | Y            | Y             | GZIP        | N           | N        |
