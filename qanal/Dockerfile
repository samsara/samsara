FROM dockerfile/java:oracle-java8


#hmm .. maybe the following should be in a script which is then run ?
ADD ./target/qanal-*-standalone.jar /opt/qanal/lib/
ADD ./resources/config.edn /opt/qanal/conf/config.edn.orig
ADD ./start-qanal.sh /opt/qanal/
RUN ln -fns /opt/qanal/lib/qanal-*-standalone.jar  /opt/qanal/qanal.jar

CMD /opt/qanal/start-qanal.sh
