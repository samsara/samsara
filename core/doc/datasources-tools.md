# datasources-tools

Here a list of requirements for the tools to manage datasources.

Firstly to clarify what datasources are:
  - data static, semi-static or streaming which is used to enrich
    another stream. Example Users database, Product catalog etc.
  - single key (id) datasets (aka primary key)
  - datasources are persisted into topic with (compact flag) on kafka
  - datasources are spilled into dynamo/cassandra/riak when not using
    kafka
  - datasources persistent format should be able to represent all
    clojure datastructure without losing fidelity.


Tools functions required:
  - create a k/v-store given a dataset (available in moebius)
  - given a tx-log create a bootstrap file
  - given a tx-log bootstrap a datasource-topic
  - given a bootstrap file, bootstrap a datasource-topic
  - given a bootstrap file, generate a update set (tx-log for update)
  - given a bootstrap file as URI bootstrap a datasource-topic


In all above functions the system which handles the persistence must
be decoupled from the functions (kafka vs kinesis vs cassandra)
