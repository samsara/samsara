# Samsara-logger
Samsara-logger is logger which provides appenders for the following logging frameworks.  
* [Log4J2 Logging API](http://logging.apache.org/log4j/2.x/)   
* [Logback](http://logback.qos.ch/)   


It will convert log messages received via the logging API into json messages which are then sent into Samsara via the [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api)

Making use of the logger's appenders simply involves adding the samsara-logger jar into the classpath of the application

## Installation
* Ensure that you have a Samsara-Ingestion-API rest server running
* Grab the latest samsara-logger jar from [clojars](https://clojars.org/samsara/samsara-logger)
  ```
      [samsara/samsara-logger "0.5.7.0"]
  ```


## Logback Usage
To use the samsara-logger Logback appender, you need to do the following
 - Ensure that the samsara-logger jar is within the application's runtime classpath
 - Create a logback.xml resource file (example below) and ensure that it is within the application's runtime classpath

An example of a logback.xml using the samsara-logger appender follows
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
 
  <appender name="STDOUT" class="samsara.logger.appenders.logback.SamsaraAppender">
    <apiUrl>http://localhost:9000</apiUrl>  

    <layout class="ch.qos.logback.classic.PatternLayout">
      <Pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</Pattern>
    </layout>
  </appender>
   
 
  <root level="debug">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

A Simple Example Application can be seen [here](./examples/logback/README.md)   


## Log4j2 Usage
To use the samsara-logger Log4j2 appender, you need to do the following
 - Ensure that the samsara-logger jar is within the application's runtime classpath
 - Create a log4j2.xml resource file (example below) and ensure that it is within the application's runtime classpath

An example of a log4j2.xml for samsara-logger follows
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN" packages="samsara.logger.appenders.log4j2">
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

A Simple Example Application can be seen [here](./examples/log4j2/README.md)   



### SamsaraAppender XML Settings

In the above logback.xml and log4j2.xml files, the *apiUrl* property is needed for the SamsaraAppender xml tag.   
The following table shows all the available configurable properties for the SamsaraAppender xml tag.  

|Appender Property | Default Value | Description |
|-------------------------|---------------|-------------|
|apiUrl      |none/nothing  | The address of the [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api), which is generally in the form ``http://<hostname>:<port>``. If this isn't set a warning is printed once and no messages are sent|
|sourceId    | One time randomly generated UUID | This value will be used as the source id for all logs sent to samsara. The source id will be used internally by samsara to ensure linearability and to aid scalable processing. If setting this please read [this](/docs/design/events-spec.md) on how to choose a good source id. |
|appId    | `<hostname>-<main/top-level class>-processid` | This is used to represent/identify this application instance. Useful when you have multiple instances of the same application running. |
|publishInterval | 5000  | This is in milliseconds and indicates how often the logger will send buffered log messages to the [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api). **WARNING** SETTING THIS TO A LOW VALUE RISKS OVERLOADING THE INGESTION API! |
|minBufferSize | 1  | This determines the minimum number of buffered log messages that is sent to the  [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api) |
|maxBufferSize | 10000  | This determines the maximum number of buffered log messages that is sent to the  [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api), older log messages are **DROPPED** |
|compression | gzip | This specifies the type of compression used to send log messages to the  [Samsara-Ingestion-API](https://github.com/samsara/samsara/ingestion-api), current compression types are **none gzip** |
|serviceName | none/nothing | If provided a serviceName property is added to all sent log events that are sent. |

A Simple Example Application can be seen [here](./examples/logback/README.md)


## License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v2.0 (http://www.apache.org/licenses/LICENSE-2.0)
