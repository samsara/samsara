# Performance profile.

This section is to prepare and run performance/load tests
against the ingestion-api.

## Target:

The target we would like to achieve is to have the main API ingestion
path (`POST /events`) to be: **less than 10ms for the 99.9%-ile** when
sending a single event in the payload.

## The setup

For the test we are using the following setup:

  * 1 zookeeper node (running on docker with boot2docker)
  * 3 kafka nodes (running on docker with boot2docker)
  * 1 ingestion-api (running on the host)
  * 1 loader node using wrk2 (by Gil Tene) with async load profile (running on the host).

## Preparing execution:

  - generate the binary executable with `lein bin`
  - start the kafka cluster with `docker-compose -f setup/docker-compose.yml up`
  - start the ingestion-api with `../../target/ingestion-api -c ./setup/config.edn`
  - then run `./bin/run-all-tests.sh`

This will run 3 times the benchmarks and write the output into `./tmp`.

The ingestion-api uses a variable number of threads to serve the requests,
so typically the first run won't be the best as threads need to ramp up.
We suggest to run them 3 times and take the best out of the three run.
The aleph thread pool which process incoming requests will grow
and shrink over time. Idle threads are killed after 10s of inactivity.
