FROM dockerfile/java:oracle-java8

ADD ./target/qanal-*-standalone.jar /opt/qanal/qanal.jar
ADD ./resources/config.edn /opt/qanal/conf/config.edn.orig
ADD ./start-qanal.sh /opt/qanal/

CMD /opt/qanal/start-qanal.sh
