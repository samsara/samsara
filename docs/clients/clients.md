---
layout: page
title: Samsara Clients
subtitle: Start publishing to Samsara in no time.
nav: documentation
tab_bar: clients
---

## Clients
Samsara provides clients for various languages and platforms. This will help you start publishing events to Samsara in no time. The clients offer the following features.

If you want to find more about the clients you can read the [clients common design principles](/docs/design/clients-design.md) document.

## Features
* Handles connectivity to the API.
* Provides API to publish events in bulk immediately.
* Provides API to buffer events locally and periodically flush to the API.
* Validates the event before publishing.
* Enriches events with default system/device parameters.
* Mobile clients will enrich events with device and location information automatically.


## Feature Matrix

| Client     | Available   | Bulk Publish | Buffer events | Compression | Device Info | Location |
|------------|-------------|--------------|---------------|-------------|-------------|----------|
| Clojure    | Y           | Y            | Y             | GZIP        | N           | N        |
| Python3    | Y           | Y            | Y             | GZIP        | N           | N        |
| iOS        | Y           | Y            | Y             | N           | Y           | Y        |
| Logger     | Y           | Y            | Y             | GZIP        | N           | N        |
