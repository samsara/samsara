# Samsara-logger
Samsara-logger is logger which implements both the [SLF4J logging API](http://slf4j.org/) and the [Log4J2 Logging API](http://logging.apache.org/log4j/2.x/)

It will convert log messages received via the logging API into json messages which are then sent into Samsara via the [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api)

Making use of the logger simply involves adding the samsara-logger jar into the classpath of the SLF4J/Log4J2 application

## Installation
* Ensure that you have a Samsara-Ingestion-API rest server running
* Grab the latest samsara-logger jar from [clojars](https://clojars.org/samsara/samsara-logger)
  ```
      [samsara/samsara-logger "0.1.1"]
  ```


## SLF4J Usage
To use the samsara-logger SLF4J implementation, you need to do the following
 - Ensure that the samsara-logger jar is within the application's runtime classpath
 - Ensure that the appropriate Samsara-logger SLF4J settings (below) are set

### Samsara-logger SLF4J Settings
Samsara-logger's SLF4J implementation can be configured via the following properties

|Environment Variable | Java System Property | Default Value | Description |
|---------------------|-----------------|---------------|-------------|
|SAMSARA_API_URL      | SAMSARA_API_URL | none/nothing  | The address of the [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api), which is generally in the form ``http://<hostname>:<port>``. If this isn't set a warning is printed once|
|SAMSARA_SOURCE_ID    | SAMSARA_SOURCE_ID | `<hostname>-<main/top-level class>-processid` | This value will be used as the source id for all logs sent to samsara. The source id will be used internally by samsara to ensure linearability and to aid scalable processing. If setting this please read [this](/docs/design/events-spec.md) on how to choose a good source id. |
|SAMSARA_LOG_TO_CONSOLE|SAMSARA_LOG_TO_CONSOLE| true    | This determines whether log messages are printed to the console |
|SAMSARA_PUBLISH_INTERVAL|SAMSARA_PUBLISH_INTERVAL| 30000  | This is in milliseconds and indicates how often the logger will send buffered log messages to the [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api). **WARNING** SETTING THIS TO A LOW VALUE RISKS OVERLOADING THE INGESTION API! |
|SAMSARA_MIN_BUFFER_SIZE|SAMSARA_MIN_BUFFER_SIZE| 1  | This determines the minimum number of buffered log messages that is sent to the  [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api) |
|SAMSARA_MAX_BUFFER_SIZE|SAMSARA_MAX_BUFFER_SIZE| 10000  | This determines the maximum number of buffered log messages that is sent to the  [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api), older log messages are **DROPPED** |

**The Java System properties takes precedence over the Environment variables**

For example if the [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api) is running locally on port 9000, you'll need to set the environment variable to `http://localhost:9000`
i.e ``export SAMSARA_API_URL=http://localhost:9000``

A Simple Example Application can be seen [here](./examples/slf4j/README.md)



## Log4j2 Usage
To use the samsara-logger Log4j2 implementation, you need to do the following
 - Ensure that the samsara-logger jar is within the application's runtime classpath
 - Create a log4j2.xml resource file (example below) and ensure that it is within the application's runtime classpath

An example of a log4j2.xml for samsara-logger follows
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN" packages="samsara.log4j2">
  <Appenders>

    <SamsaraAppender name="Samsara" apiUrl="http://localhost:9000">
    </SamsaraAppender>

  </Appenders>
  <Loggers>
    <Root level="debug">
      <AppenderRef ref="Samsara"/>
    </Root>
  </Loggers>
</Configuration>
```

In the example above there 2 important things to take note of -
 - The Configuration xml element has it's **packages** property pointing to the samsara package ``samsara.log4j2``
 - There's a **SamsaraAppender** xml element *AND* it has at least the **apiUrl** property set.

### SamsaraAppender Properties
In the above log4j2.xml, there was a property (apiUrl) set for the SamsaraAppender.
The following table shows all the available configurable properties for SamsaraAppender

|SamsaraAppender Property | Default Value | Description |
|-------------------------|---------------|-------------|
|apiUrl      |none/nothing  | The address of the [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api), which is generally in the form ``http://<hostname>:<port>``. If this isn't set a warning is printed once|
|sourceId    | `<hostname>-<main/top-level class>-processid` | This value will be used as the source id for all logs sent to samsara. The source id will be used internally by samsara to ensure linearability and to aid scalable processing. If setting this please read [this](/docs/design/events-spec.md) on how to choose a good source id. |
|logToConsole | true    | This determines whether log messages are printed to the console |
|publishInterval | 30000  | This is in milliseconds and indicates how often the logger will send buffered log messages to the [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api). **WARNING** SETTING THIS TO A LOW VALUE RISKS OVERLOADING THE INGESTION API! |
|minBufferSize | 1  | This determines the minimum number of buffered log messages that is sent to the  [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api) |
|maxBufferSize | 10000  | This determines the maximum number of buffered log messages that is sent to the  [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api), older log messages are **DROPPED** |

A Simple Example Application can be seen [here](./examples/log4j2/README.md)

## License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v2.0 (http://www.apache.org/licenses/LICENSE-2.0)
