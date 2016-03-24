# Qanal state machine

Qanal design  goal is to  make a system  which is highly  resilient to
many different  type of failures.   For a successful  operation, qanal
needs  to  communicate   with  several  systems  such   as  Kafka  and
ElasticSearch.  Both  systems typically are  deployed in a  cluster of
machines.   In addition  to that  it is  common to  have ElasticSearch
RESTful interface behind a LoadBalancer.  In such setup many different
errors can arise. We tried to list  all major class of errors.  By any
means  this is  an exhaustive  list of  possible error,  but just  the
common ones.   We will also  see how  we can handle  unexpected errors
which are not listed here.

Common operational errors:

| Error Code | Description                   | Action                            |
|-----------:|-------------------------------|-----------------------------------|
|          1 | Cannot talk to Kafka          | Retry                             |
|          2 | Topic doesnt exist            | Retry                             |
|          3 | Partition doesnt exist        | Retry                             |
|          4 | Message too big               | Send to Error                     |
|          5 | Offset doesnt exist           | Action - Reset to earliest/latest |
|          6 | Network exceptions            | Retry                             |
|          7 | Message format Invalid        | Send to Error                     |
|          8 | Invalid Event                 | Send to Error                     |
|          9 | Transformation Error          | Send to Error                     |
|         10 | Cannot talk to ES             | Retry                             |
|         11 | ES Error (5xx)                | Retry                             |
|         12 | Bulk response contains errors | Send to Error                     |
|         13 | ES Error (4xx)                | Retry                             |


In order  to understand how  the system can  recover in face  of these
errors  we decided  to  build  the qanal  core  using  a Finite  State
Automata (or State Machine).  A  State Machine give us the possibility
to define how to handle a given error in relation to the current state
of the system. Another great property  of State Machine models is that
the _current_ state  is typically inspectable. In other  words you can
connect to a running system and see what is going on right now, and in
which state the System is. This  property comes very useful to extract
and publish processing metrics as the only  thing you need to do is to
sample  the current  state and  push the  information to  a monitoring
system.

Here is the qanal's state machine.

![State Machine](./images/state-machine.png)

This is for a single thread consumer.  If we are consuming more topics
and  more partitions  then  we  can have  a  state  machine for  every
processing thread.

![Multi State Machine](./images/multi-state-machine.png)

The `configure` step would read the given configuration and decide how
many consumer threads  are required for the processing and  spin up an
appropriate number of dedicated threads.
