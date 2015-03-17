FROM dockerfile/java:oracle-java8

#RUN apt-get update
#RUN apt-get install -y supervisor
#ADD ./docker/supervisord.conf /etc/supervisor/supervisord.conf
#ADD ./docker/qanal-supervisor.conf /etc/supervisor/conf.d/
#ADD ./docker/start-supervisord.sh /

ADD ./target/qanal-*-standalone.jar /opt/qanal/qanal.jar
ADD ./resources/config.edn /opt/qanal/conf/config.edn.orig
ADD ./docker/start-qanal.sh /opt/qanal/

#CMD /start-supervisord.sh
CMD /opt/qanal/start-qanal.sh
