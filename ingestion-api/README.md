# samsara-ingestion-api

Ingestion APIs for Samsara's analytics


## Spec

API specifications are in Swagger 2.0 format.

you can edit the specification with the Swagger editor

```
git clone git@github.com:swagger-api/swagger-editor.git
cd swagger-editor
npm start
```

and then import the [spec/ingestion-api-spec.yaml](spec/ingestion-api-spec.yaml)

## How to build it

This projects uses [lein-bin plugin](https://github.com/Raynes/lein-bin) which
is required to be installed.

```
lein do clean, bin
```

The `lein-bin` will produce an executable uberjar in `./target/ingestion-api`.

## How to run it

```
-----------------------------------------------
   _____
  / ___/____ _____ ___  _________ __________ _
  \__ \/ __ `/ __ `__ \/ ___/ __ `/ ___/ __ `/
 ___/ / /_/ / / / / / (__  ) /_/ / /  / /_/ /
/____/\__,_/_/ /_/ /_/____/\__,_/_/   \__,_/

  Samsara Ingestion API
-----------------------------| v0.0.0  |-------


SYNOPSIS
       ./ingestion-api -c config.edn

DESCRIPTION
       Starts a REST interface which accepts events
       and sends them to Samsara's processing pipeline.

  OPTIONS:

  -c --config config-file.edn [REQUIRED]
       Configuration file to set up the REST interface
       example: './config/config.edn'

  -h --help
       this help page

```

You must pass a configuraiton file in the following way:
```
./target/ingestion-api -c ./config/config.edn
```

The configuration looks as follow:

```
{
  :server {:port 9000 :auto-reload false}

  :admin-server {:port 9010 :auto-reload false}

  :log   {:timestamp-pattern "yyyy-MM-dd HH:mm:ss.SSS zzz"}

  :backend  {:type :console :pretty? true}
}
```

Here is the description of the configuration

  * `:server` - This section controls the `http` server settings
      * `:port` - Set listnening port for the server
      * `:auto-reload` - If set to true, it watches for changes in the source files
                       and automatically reloads the changes without requiring
                       server reloads.
                       This should be used only used in development.

  * `:log` - This section controls the logging settings.
      * `:timestamp-pattern` - timestamp pattern for the log.
      * Any other [timbre logging](https://github.com/ptaoussanis/timbre) configuration.

  * `:backend` - This section controls the backend configuarion where the events are sent.
      * `:type` - This can be one of: `:console` or `:kafka`

Depending on the backend type you can have different options:

  * `:type :console` - Prints all the events to the stdandard output.
                     This is mostly used for testing purposes only.
      * `:pretty?` can be `true` or `false` and defines if it is pretty printed or simple `println`.

  * `:type :kafka` - Sends the events to a Kafka topic in JSON format.
      * `:topic` - is the name of the Kafka topic where to send the events to. (Default: `events`, *REQUIRED*)
      * `:metadata.broker.list` - This is a comma-separated-list of brokers host or IPs and their port. (*REQUIRED*)
                               Example: `172.0.0.3:9092,172.0.0.4:9092,172.0.0.5:9092`
                               It support [synapse](https://github.com/BrunoBonacci/synapse) style
                               links so you could write something like `:metadata.broker.list "%%>>kafka.*:9092%%"`
                               and it will be replaced with the actual instance ips and ports using
                               Docker's environment variables.
      * You can put additional Kafka Producer configuration options as from [Kafka options](http://kafka.apache.org/documentation.html#producerconfigs)

Here is the default configuration for the Kafka producer:

| Kafka configuration option  |          Default value              |
| --------------------------- | ----------------------------------- |
| "serializer.class"          | "kafka.serializer.StringEncoder"    |
| "partitioner.class"         | "kafka.producer.DefaultPartitioner" |
| "request.required.acks"     | "-1"                                |
| "producer.type"             | "async"                             |
|  "message.send.max.retries" | "5"                                 |


## Tracking of metrics

To track metrics I use [TRACKit!](https://github.com/samsara/trackit)
and we expose the following metrics:

```
# tracking the rate of events received

ingestion.mqtt.events
ingestion.http.events
             count = total count of events received
         mean rate = mean rate events/second
     1-minute rate = mean rate events/second over last minute
     5-minute rate = mean rate events/second over last 5 minutes
    15-minute rate = mean rate events/second over last 15 minutes


# tracking the rate of HTTP or MQTT requests received (which may contain 1 or more events)

ingestion.mqtt.requests
ingestion.http.requests
             count = total count of requests received
         mean rate = mean rate requests/second
     1-minute rate = mean rate requests/second over last minute
     5-minute rate = mean rate requests/second over last 5 minutes
    15-minute rate = mean rate requests/second over last 15 minutes


# tracking the rate of  MQTT requests received containing 1 or more events which failed the validation

ingestion.mqtt.requests
             count = total count of requests received
         mean rate = mean rate requests/second
     1-minute rate = mean rate requests/second over last minute
     5-minute rate = mean rate requests/second over last 5 minutes
    15-minute rate = mean rate requests/second over last 15 minutes

# tracking the distribution (histogram) of http request batches

ingestion.mqtt.batch.size
ingestion.http.batch.size
             count = total count of requests received
               min = minimum number of events per request
               max = maximum number of events per request
              mean = average number of events per request
            stddev = standard deviation of number of events per request
            median = median of number of events per request
              75% <= various percentiles of number of events per request
              95% <=              "
              98% <=              "
              99% <=              "
            99.9% <=              "


# tracking how long it takes to validate the batch of events

ingestion.batch.validation
             count = number of batch validations
         mean_rate = mean rate x second
           m1_rate = rate x second over last 1 minute
           m5_rate = rate x second over last 5 minutes
          m15_rate = rate x second over last 15 minutes
               min = min execution time
               max = max execution time
              mean = mean execution time
            stddev = standard deviation on execution time
            median = mean execution time
              p75 <= various percentiles on execution time
              p95 <=               ''
              p98 <=               ''
              p99 <=               ''
             p999 <=               ''


# tracking how long it takes to send the batch of events to the selected backend

ingestion.batch.backend-send
             count = number of events batch sent
         mean_rate = mean rate x second
           m1_rate = rate x second over last 1 minute
           m5_rate = rate x second over last 5 minutes
          m15_rate = rate x second over last 15 minutes
               min = min execution time
               max = max execution time
              mean = mean execution time
            stddev = standard deviation on execution time
            median = mean execution time
              p75 <= various percentiles on execution time
              p95 <=               ''
              p98 <=               ''
              p99 <=               ''
             p999 <=               ''

```

## How to build and run the Docker container

A `Dockerfile` is available for this project. To build it just run:

```
lein do clean, bin
docker build -t samsara/ingestion-api .
```
Then to run the container:

```
docker run -d -p 9000:9000 -p 15000:15000 -v /tmp/ingestion-api:/logs \
        --link=kafkadocker_kafka_11:kafka_1 \
        --link=kafkadocker_kafka_12:kafka_2 \
        --link=kafkadocker_kafka_13:kafka_3 \
        samsara/ingestion-api
```

The linked containers will then be used for configuration autodiscovery.

This will expose port `9000` and port `15000` on your docker host. It will mount
a volume to expose the container logs as `/tmp/ingestion-api`.

You should be able to point your browser to [http://127.0.0.1:15000] and log in with
`admin` / `samsara` to access supervisord web console.

**NOTE: if you are running on OSX with `boot2docker` the host on wich will the ports be exposed
won't be the local host but the `boot2docker` virtual machine which by default is on
`192.168.59.103` (check your $DOCKER_HOST).** This means that instead of accessing
[http://127.0.0.1:15000] you will have to access [http://192.168.59.103:15000] and
that the mounted volume (`/tmp/ingestion-api`) will be on the `boot2docker` virtual machine.
To access them you can log into `boot2docker` with the following command: `boot2docker ssh`.

## Available endpoints

For more information about the available endpoints please refer to
[ingestion-api documentaiton](/docs/design/ingestion-api.md)

## License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
