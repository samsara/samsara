#!/bin/bash
# adapted from https://github.com/wurstmeister/kafka-docker/blob/master/download-kafka.sh

mirror=$(curl --stderr /dev/null https://www.apache.org/dyn/closer.cgi\?as_json\=1 | sed -rn 's/.*"preferred":.*"(.*)"/\1/p')
url="${mirror}kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz"
curl -sSL "${url}" | tar -zxv -C /opt
ln -fs /opt/kafka_* /opt/kafka
