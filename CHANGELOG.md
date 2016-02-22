# Samsara changelog


## version 0.5.5.0 (2016-01-23)

This is the first version after the unification of all sub-projects and it matches
the last release of the individual projects.
The docker images pushed into DockerHub have been tagged as they were.

  * First release after the unification.
  * There should be no functional changes since the individual projects releases.


## version 0.5.6.0 (2016-01-24)

  * updated base docker image to ubuntu15.10 and new versions of JDK7/8

## version 0.5.7.0 (2016-01-25)

  * update monitoring docker image with: Riemann-0.2.10, InfluxDB-0.9.6.1, MySQL-5.7.10, Grafana 2.6.0

## version 0.5.7.1 (2016-02-22)

  * update monitoring docker image with: InfluxDB-0.10.0-1
  * added default monitoring dashboards
  * monitoring image migrated to Alpine Linux

## version 0.6.0.0 (not released yet)

  * all containers migrated to Alpine Linux for small footprint
  * GOALS:
    - update all third-party dependencies
    - qanal elastic scale on new kafka client
    - core elastic scale
    - core execution model (without samza)
    - ingestion registry
