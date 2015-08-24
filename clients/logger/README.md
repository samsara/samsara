
# Samsara-logger
Samsara-logger is logger which implements both the [SLF4J logging API](http://slf4j.org/) and the [Log4J2 Logging API](http://logging.apache.org/log4j/2.x/)  

It will convert log messages received via the logging API into json messages which are then sent into Samsara via the [Samsara-Ingestion-API](https://github.com/samsara/samsara-ingestion-api)  

Making use of the logger simply involves adding the samsara-logger jar into the classpath of the SLF4J/Log4J2 application  

## Installation
* Ensure that you have a Samsara-Ingestion-API rest server running
* Grab the latest samsara-logger jar from [clojars](https://clojars.org/samsara/samsara-logger)  
  ```clojure  
      [samsara/samsara-logger "0.1.0-beta"]  
  ```  


## SL4J Usage
Ensure that the samsara-logger jar is with in the application's classpath, also ensure that the *SAMSARA_API_URL* environment variable/System property is set.   
*If the java system property is set, it overrides the evironment variable*  

For example if the [Samsara-Ingestion-API](https://github.com/samsara/samsara-ingestion-api) is running locally on port 9000, you'll need to set the environment variable to `http://localhost:9000/v1`   
i.e ``export SAMSARA_API_URL=http://localhost:9000/v1``

A Simple Example Application can be seen [here](./examples/slf4j/README.md)  

## Log4j2 Usage
Ensure that the samsara-logger jar is within the applicaiton's classpath and that you have a log4j2 resouce xml file within the classpath.  
An example of a log4j2.xml for samsara-logger follows   
```xml  
<?xml version="1.0" encoding="UTF-8"?>  
<Configuration status="WARN" packages="samsara.log4j2">  
  <Appenders>  
  
    <SamsaraAppender name="Samsara" apiUrl="http://localhost:9000/v1">  
    </SamsaraAppender>  
  
  </Appenders>  
  <Loggers>  
    <Root level="debug">  
      <AppenderRef ref="Samsara"/>  
    </Root>  
  </Loggers>  
</Configuration>  
```  

_Please note that the Configuration element has it's packages attribute set to **samsara.log4j2**_

A Simple Example Application can be seen [here](./examples/log4j2/README.md)  

## License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v2.0 (http://www.apache.org/licenses/LICENSE-2.0)
