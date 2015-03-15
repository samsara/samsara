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
-----------------------------| v0.1.0- |-------

ERROR: Missing configuration.

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
      * `http-kit` additional configuration options available [here](http://www.http-kit.org/server.html#options) 
    
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
      * `:metadata.broker.list` - This is a comma-separated-list of brokers host or IPs and their port.
                               Example: `172.0.0.3:9092,172.0.0.4:9092,172.0.0.5:9092`
      * You can put additional Kafka Producer configuration options as from [Kafka options](http://kafka.apache.org/documentation.html#producerconfigs)

Here is the default configuration for the Kafka producer:

| Kafka configuration option  |          Default value              |
| --------------------------- | ----------------------------------- |
| "serializer.class"          | "kafka.serializer.StringEncoder"    |
| "partitioner.class"         | "kafka.producer.DefaultPartitioner" |
| "request.required.acks"     | "-1"                                |
| "producer.type"             | "async"                             |
|  "message.send.max.retries" | "5"                                 |

## License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
