# Log4j2 Example

This is a very simple Application showing how to integrate and use the samsara-logger.

## Usage

To use the Log4j2 implementation within samsara-logger, you'll make sure that samsara-logger is within the runtime classpath of your application.
The latest samsara-logger can be grabbed from [clojars](https://clojars.org/samsara/samsara-logger)

### Leiningen
Add the following to your project.clj's dependencies Vector
```
[samsara/samsara-logger "0.1.1"]
```

### Gradle
Add the following to your build.gradle file
```
//Ensure you have maven central repo for log4j2 and a maven clojars repo for samsara-logger
repositories {
    mavenCentral()
    maven { url "https://clojars.org/repo"}
}

//Ensure you have log4j2 compile dependencies and samsara-logger runtime dependency
dependencies {
    compile 'org.apache.logging.log4j:log4j-api:2.3',
            'org.apache.logging.log4j:log4j-core:2.3'

    runtime 'samsara:samsara-logger:0.1.1'
}
```

### Maven
Add the following to your pom.xml file
```xml
<!-- Place the following in the repositories section -->
<repository>
  <id>clojars</id>
  <url>http://clojars.org/repo/</url>
</repository>

<!-- Place the following in the dependencies section -->
<dependency>
  <groupId>samsara</groupId>
  <artifactId>samsara-logger</artifactId>
  <version>0.1.1</version>
</dependency>
```

## Running the Example

### Run the ingestion api (in console/dev mode)
  - Clone the Ingestion api project ``git clone https://github.com/samsara/samsara.git``
  - go to the subproject `ingestion-api`
  - Build it ``lein do clean, bin``
  - Run it (in console/dev mode) ``./target/ingestion-api -c ./config/config.edn``

### Run the Example
  - In another console, in this directory (examples/log4j2), then run the example ``gradle clean run``
