---
layout: page
title: Events Specification
subtitle: Quick overview of the Event Structure
nav: documentation
---
## Samsara Events

Samsara Events are represented in simple JSON format.

```javascript
 {
   "timestamp": 1433004485870,
   "sourceId": "3aw4sedrtcyvgbuhjkn",
   "eventName": "session.started"
 }
```

The following fields are mandatory:

  * *timestamp* - Epoch time in millseconds when the event occurred.
  * *eventName* - Name of the event.
  * *sourceId*  - String that uniquely identifies the client.

### Event Name

We recommend naming events as actions that happened in the past. For eg: 

Examples of *good* Event Names:

  * session.began
  * homebutton.clicked
  * notifications.activated
  * cardpayment.submitted

Examples of *bad* Event Names:

  * login_request
  * payment


We recommend using lower case strings separated by a '.' for readability and consistency.

### SourceID

The sourceId must be provided. **It is important to select carefully your client ID, as all events with the same `sourceId` will be routed to the same server and same thread.**.
This property is important to provide linearizability of events coming from the same client.

In order to select a good `sourceId` you have to look for the following properties.

  - **high cardinality** - which will help the system to scale
  - **non randomized** - the same id must be used by the same source over time.
  - **unique** - You must be able to identify uniquely a source from a given ID.
  
Here some examples of **good** choices for `sourceId`:

  - a device id for a mobile application
  - a customer or client id for a web application
  - a userid for a web service

Here some examples of **BAD** bad choices for `sourceId`:

  - a web service name, because it is a low cardinality. This means that all events
    coming from a particular webservice will be processed by a single thread.
    A busy webservice can do hundreds of millions or billions of requests per day,
    which in this case will be all queued to be processed by a single thread.
    Replace the webservice name with the clientId of the webservice user,
    or the sessionId or if you really want to use name append the process id (PID)
    to the name (such as: "com.example.api.user-service:56789")
  - a randomly generate id which is not persitsed and regenerated on every use.
    This is bad because it doesn't allow you to trace an history of the events
    and make meaningful correlations.
  - same thing will happen if the sourceId is not unique. Events from multiple different
    sources will mix together generating an undistiguishable events soup

