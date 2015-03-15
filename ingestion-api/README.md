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


