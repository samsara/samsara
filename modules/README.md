# samsara-bultin-modules

This projects contains Samsara's built-in modules.

## Available modules

  * [Session management](#session-management)
    * [Auto detect session duration from start/stop events](#session-boundaries-correlation).
    * TODO: Sessionize web requests
  * Miscellaneous
    * TODO: IP geo-location
  * Twitter
    * TODO: Sentiment analysis

## Session management

### Session boundaries correlation

This modules enables you to detect events which mark the beginning and the end
of an activity and automatically compute the duration of the session.

Common examples are the **start**/**stop** of a game play or when the an app
**start**/**stop** downloading a large payload, or anything which gets
**plugged**/**unplugged**, **open**/**closed**, **connected**/**disconnected**,
**enabled**/**disabled** or **activated**/**deactivated**. Any sort of logical
pair can be used to compute the duration of that particular session.

Pairs (or session gates) can be configured with what make sense for your case.
By default we detect only events whose name terminates by **.start**/**.stopped**,
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

For more info check: [Session boundaries](/doc/session-boundaries-module.md)

## License

Copyright © 2015-2017 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
