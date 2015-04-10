(ns qanal.kafka-test
  (:use midje.sweet)
  (:import [kafka.javaapi.consumer SimpleConsumer])
  (:require [qanal.kafka :refer :all]
            [clj-kafka.consumer.simple :refer [consumer topic-meta-data messages topic-offset]]
            [clj-kafka.zk :refer [brokers]]))

(facts "About get-lead-broker"
       (let [known-partition-req {:host         ..localhost.. :port ..2181.. :topic ..test-topic..
                                  :partition-id ..9.. :group-id ..qanal-id..}
             unknown-partition-req (assoc known-partition-req :partition-id ..99..)
             correct-broker {:connect "localhost:9092" :host "localhost" :port 9092 :broker-id 2}
             partition-meta-seq (list {:partition-id ..8.. :leader ..incorrect-broker.. :replicas ..broker-list1.. :in-sync-replicas ..broker-list2.. :error-code ..0..}
                                      {:partition-id ..9.. :leader correct-broker :replicas ..broker-list1.. :in-sync-replicas ..broker-list2.. :error-code ..0..}
                                      {:partition-id ..10.. :leader ..incorrect-broker.. :replicas ..broker-list1.. :in-sync-replicas ..broker-list2.. :error-code ..0..})
             topic-meta-seq (list {:topic ..test-topic.. :partition-metadata partition-meta-seq})]

         (fact "If a known partition-id (within the topic meta data) is requested,
              the correct broker infomation (map) is returned."

               (get-lead-broker known-partition-req) => correct-broker
               (provided
                 (consumer ..localhost.. ..2181.. ..qanal-id..) => ..consumer..
                 (topic-meta-data ..consumer.. [..test-topic..]) => topic-meta-seq))

         (fact "If an unknown partition-id is requested, nil is returned"
               (get-lead-broker unknown-partition-req) => nil
               (provided
                 (consumer ..localhost.. ..2181.. ..qanal-id..) => ..consumer..
                 (topic-meta-data ..consumer.. [..test-topic..]) => topic-meta-seq))))


(facts "About connect-to-lead-broker"
       (let [partition-req {:topic             ..test-topic.. :partition-id ..known-partition..
                            :zookeeper-connect ..zookeeper-connect.. :group-id ..qanal-id..}
             broker-A {:host ..host-A.. :port ..9092..}
             broker-B {:host ..host-B.. :port ..9092..}
             expected-request {"zookeeper.connect" ..zookeeper-connect..}
             lead-broker (assoc broker-A :connect ..connect-string.. :broker-id ..2..)]

         (fact "If a lead broker is found for a requested partition-id, then a
              kafka.javaapi.consumer.SimpleConsumer is returned."
               (connect-to-lead-broker partition-req) => #(instance? SimpleConsumer %)
               (provided
                 (brokers expected-request) => (list broker-B broker-A)
                 (get-lead-broker anything) => lead-broker
                 (consumer ..host-A.. ..9092.. ..qanal-id..) => (SimpleConsumer. "localhost" 9092 5000 100 "test-client")))

         (fact "If a lead broker isn't found then nil is returned."
               (connect-to-lead-broker partition-req) => nil
               (provided
                 (brokers expected-request) => (list broker-B broker-A)
                 (get-lead-broker anything) => nil))))


(facts "About calculate-partition-backlog"
       (fact "If the broker query for last offset succedes then calculate backlog"
             (calculate-partition-backlog ..consumer.. {:consumer-offset 101}) => 10
             (provided
               (get-latest-topic-offset anything anything) => 111))

       (fact "If the broker query fails with an exception then -1 is returned"
             (calculate-partition-backlog ..consumer.. {:onsumer-offset 101}) => -1
             (provided
               (get-latest-topic-offset anything anything) =throws=> (Exception. "oops ^_^"))))
