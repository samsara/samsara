
# Samsara-logger
Samsara-logger is logger which implements both the [SLF4J logging API](http://slf4j.org/) and the [Log4J2 Logging API](http://logging.apache.org/log4j/2.x/)  

It will convert log messages received via the logging API into json messages which are then sent into Samsara via the [Samsara-Ingestion-API](https://github.com/samsara/samsara-ingestion-api)  

Making use of the logger simply involves adding the samsara-logger jar into the classpath of the SLF4J/Log4J2 application  

## Installation
* Ensure that you have a Samsara-Ingestion-API rest server running
* Grab the latest samsara-logger jar   


## SL4J Usage
Ensure that the samsara-logger jar is with in the application's classpath, also ensure that the *SAMSARA_API_URL* environment variable is set.   

For example if the [Samsara-Ingestion-API](https://github.com/samsara/samsara-ingestion-api) is running locally on port 9000, you'll need to set the environment variable to `http://localhost:9000/v1`   
i.e ``export SAMSARA_API_URL=http://localhost:9000/v1``

## Log4j2 Example
TO DO 

### TODO
Rewrite in Java

## License

Copyright © 2015 Samsara's authors.

Distributed under the Apache License v2.0 (http://www.apache.org/licenses/LICENSE-2.0)
