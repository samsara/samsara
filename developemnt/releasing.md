# How to make a release of Samsara.

Minor releases and patch releases can be done individually. Please see
the [version scheme](/VERSIONS.md) for more information.

For a major release follow these steps.

## Vote the release
The release must be agreed by all developers in Slack channel.

## Bump the version number
Increment the version by updating the [samsara.version](/samsara.version) file.


## Build and test all projects
Make sure that all projects actually build and all tests are passing:

    ./bin/build-all.sh -test


## Release the libraries to Clojars.org

One by one release all the libraries

    cd samsara
    export SAMSARA=`pwd`

    cd $SAMSARA/utils
    lein deploy clojars

    cd $SAMSARA/clients/clojure
    lein deploy clojars

    cd $SAMSARA/clients/logger
    lein deploy clojars

    cd $SAMSARA/moebius
    lein deploy clojars

    cd $SAMSARA/modules
    lein deploy clojars


## Build and release the containers

### Third-party containers

Now build and release the third-party containers and tag with both:
their upstream release and the samsara's version, for example:

    docker tag aa259a6f4417 samsara/kafka:kafka-0.8.2.1   # upstream kafka version
    docker tag aa259a6f4417 samsara/kafka:0.5.5.0         # samsara version
    docker tag aa259a6f4417 samsara/kafka                 # latest tag

If the upstream version tag is already used append a letter a the end
such as `samsara/kafka:kafka-0.8.2.1b` And push them all.

For each container, update the projects `dockerinfo.md` and copy/paste
in the DokcerHub page.

### Samsara containers

Now build and release the samsara's containers

    cd $SAMSARA/ingestion-api
    lein do clean, bin
    docker build -t samsara/ingestion-api:`cat $SAMSARA/samsara.version` .
    # move the latest tag
    docker tag $docker images  | grep samsara/ingestion-api | grep `cat $SAMSARA/samsara.version` | awk '{print $3}') samsara/ingestion-api
    docker push samsara/ingestion-api:`cat $SAMSARA/samsara.version`
    docker push samsara/ingestion-api


    cd $SAMSARA/qanal
    lein do clean, bin
    docker build -t samsara/qanal:`cat $SAMSARA/samsara.version` .
    # move the latest tag
    docker tag $docker images  | grep samsara/qanal | grep `cat $SAMSARA/samsara.version` | awk '{print $3}') samsara/qanal
    docker push samsara/qanal:`cat $SAMSARA/samsara.version`
    docker push samsara/qanal


    cd $SAMSARA/core
    lein do clean, bin
    docker build -t samsara/samsara-core:`cat $SAMSARA/samsara.version` .
    # move the latest tag
    docker tag $docker images  | grep samsara/samsara-core | grep `cat $SAMSARA/samsara.version` | awk '{print $3}') samsara/samsara-core
    docker push samsara/samsara-core:`cat $SAMSARA/samsara.version`
    docker push samsara/samsara-core


For each container, update the projects `dockerinfo.md` and copy/paste
in the DokcerHub page.

### docker-compose and bootstrap script.

Update the docker-compose descriptor to have all images use the new version.

    cd $SAMSARA/docker-images/bootstrap
    gsed -i "/image:/s/$/:`cat $SAMSARA/samsara.version`/g" docker-compose.yml
    gsed -i "/image:/s/$/:`cat $SAMSARA/samsara.version`/g" docker-compose-with-spark.yml

Until a more permanent solution is put in place, make sure that the docker-compose
files point the right version:


    gsed -i "/command:.*bootstrap.sh/s/master/`cat $SAMSARA/samsara.version`/g" docker-compose.yml
    gsed -i "/command:.*bootstrap.sh/s/master/`cat $SAMSARA/samsara.version`/g" docker-compose-with-spark.yml

## Update changelog

Make sure that the [CHANGELOG](/changelog.md) file is up-to-date.

## Commit and tag

Double check that all changes you made are ok for the release. Then
commit and push.

## Post release

Restore the docker compose version to latest and bootstrap to master

    cd $SAMSARA/docker-images/bootstrap
    gsed -i "/image:/s/:`cat $SAMSARA/samsara.version`//g" docker-compose.yml
    gsed -i "/image:/s/:`cat $SAMSARA/samsara.version`//g" docker-compose-with-spark.yml
    gsed -i "/command:.*bootstrap.sh/s/`cat $SAMSARA/samsara.version`/master/g" docker-compose.yml
    gsed -i "/command:.*bootstrap.sh/s/`cat $SAMSARA/samsara.version`/master/g" docker-compose-with-spark.yml

Bump the `samsara.version` minor release number (third digit) and
append `-SNAPSHOT`. For example:

    0.5.5.0 --becomes--> 0.5.6.0-SNAPSHOT

Finally make an announcement of the new release in Slack and other channels.
