# Legislate


## Introduction
Legislate is a way to reason on how to coordinate, read & update values across

different processes using a stream of messages, provided that all participating processes

eventually see the messages in the **same order**.


## Characteristics
To help focus and illustrate what we are talking about, it is useful to define certain
charecteristics of the system.

 * The participating processes can be located anywhere (different machines, subnets, datacenters)
 * Participating processes are guaranteed to see the **same order** of messages eventually
 * Participating processes aren't required to be able to directly communicate with each other,
   in fact it is assumed that they can't directly communicate with each other.
 * The first message, seen in the stream, overrides all subsquent messages of the same
   type/key and version.


