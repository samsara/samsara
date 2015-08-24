# Slf4j Example

This is a very simple Application showing how to integrate and use the samsara-logger.  

## Usage

To use the Slf4J implementation within samsara-logger, you'll make sure that samsara-logger is within the runtime classpath of your application.  
The latest samsara-logger can be grabbed from [clojars](https://clojars.org/samsara/samsara-logger)   

### Leiningen
Add the following to your project.clj's dependencies Vector  
```  
[samsara/samsara-logger "0.1.0-beta"]  
```  

### Gradle
Add the following to your build.gradle file  
```  
//Ensure you have maven central repo for slf4j and a maven clojars repo for samsara-logger   
repositories {  
    mavenCentral()  
    maven { url "https://clojars.org/repo"}  
}  

//Ensure you have slf4j compile dependency and samsara-logger runtime dependency  
dependencies {  
    compile 'org.slf4j:slf4j-api:1.7.12'  

    runtime 'samsara:samsara-logger:0.1.0-beta'  
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
  <version>0.1.0-beta</version>   
</dependency>   
```   

## Running the Example

### Run the ingestion api (in console/dev mode)  
  - Clone the Ingestion api project ``git clone https://github.com/samsara/samsara-ingestion-api.git``   
  - Build it ``lein do clean, bin``  
  - Run it (in console/dev mode) ``./target/ingestion-api -c ./config/config.edn``  

### Run the Example
  - In another console, in this directory (examples/slf4j), set the environment variable ``export SAMSARA_API_URL=http://localhost:9000/v1``  
  - Then run the example ``gradle clean run``   

