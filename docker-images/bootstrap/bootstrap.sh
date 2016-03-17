#!/bin/bash

# log output of this script
exec > >( tee -a /logs/bootstrap.out )
exec 2>&1

function wait_for {
   echo "Checking if $1 is started."

   while [ "$(nc -z -w 5 $2 $3 || echo 1)" == "1" ] ; do
       echo "Waiting for $1 to start up..."
       sleep 3
   done
}


function create_topic {
    CONFIG=""
    # $1 - name of the topic
    # $2 - "compact" for compaction (OPTIONAL)
    [ "$2" = "compact" ] && CONFIG="--config cleanup.policy=compact"
    # if the topic doesn't exist create one
    echo "checking if topic $1 is created ($CONFIG)"
    /opt/kafka/bin/kafka-topics.sh \
        --zookeeper $ZOOKEEPER_1_PORT_2181_TCP_ADDR \
        --describe \
        | grep -q "Topic: $1\s" \
        ||  /opt/kafka/bin/kafka-topics.sh \
                --zookeeper $ZOOKEEPER_1_PORT_2181_TCP_ADDR \
                --create --topic $1 \
                $CONFIG \
                --replication-factor 1 \
                --partitions 5

}


wait_for zookeeper $ZOOKEEPER_1_PORT_2181_TCP_ADDR 2181
wait_for kafka     $KAFKA_PORT_9092_TCP_ADDR       9092


create_topic ingestion
create_topic ingestion-errors
create_topic ingestion-kv compact
create_topic events

# list topic
/opt/kafka/bin/kafka-topics.sh \
    --zookeeper $ZOOKEEPER_1_PORT_2181_TCP_ADDR \
    --describe

# now create ELS index
wait_for ElasticSearch $ELASTICSEARCH_PORT_9200_TCP_ADDR 9200


echo "checking if events index is created"
curl -sSL "http://$ELASTICSEARCH_PORT_9200_TCP_ADDR:9200/_cat/indices?v" \
    | grep -q 'events' \
    || curl -sSL -XPUT "http://$ELASTICSEARCH_PORT_9200_TCP_ADDR:9200/events" \
            -d '{
    "settings" : {
        "number_of_shards" : 3,
        "number_of_replicas" : 0
    }
}'


echo "changing the number of replicas"
curl -sSL http://$ELASTICSEARCH_PORT_9200_TCP_ADDR:9200/_cat/indices \
    | awk '{print $3}' \
    | xargs -I{} \
            curl -sS -XPUT http://$ELASTICSEARCH_PORT_9200_TCP_ADDR:9200/{}/_settings \
            -d '{"index":{ "number_of_replicas": 0}}'


echo "checking if kibana index is created"
curl -sSL "http://$ELASTICSEARCH_PORT_9200_TCP_ADDR:9200/_cat/indices?v" \
    | grep -q 'kibana' \
    || curl -sSL -XPUT "http://$ELASTICSEARCH_PORT_9200_TCP_ADDR:9200/kibana" \
            -d '{
    "settings" : {
        "number_of_shards" : 1,
        "number_of_replicas" : 0
    }
}'


# list indices
curl -sSL "http://$ELASTICSEARCH_PORT_9200_TCP_ADDR:9200/_cat/indices?v"


echo ''
echo ''
echo ';;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;'
echo ';;                                                                            ;;'
echo ';;    ---==| S A M S A R A   I S   R E A D Y   F O R   A C T I O N |==----    ;;'
echo ';;                                                                            ;;'
echo ';;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;'
echo ''
echo ''


# now just pause
nc -kl 12345
