FROM ubuntu:14.10

MAINTAINER Samsara's team (https://github.com/samsara/samsara-docker-images)

RUN apt-get update
RUN apt-get install -y wget curl


#
# JDK installation
#

ENV SJDK7 http://download.oracle.com/otn-pub/java/jdk/7u75-b13/jdk-7u75-linux-x64.tar.gz
ENV SJDK8 http://download.oracle.com/otn-pub/java/jdk/8u40-b26/jdk-8u40-linux-x64.tar.gz

RUN wget -nv --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" $SJDK7  -O /tmp/java.tgz
RUN mkdir -p /usr/lib/jvm && \
    tar zxvf /tmp/java.tgz -C /usr/lib/jvm
RUN update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/*/bin/javac 1       && \
    update-alternatives --install /usr/bin/java java /usr/lib/jvm/*/bin/java 1          && \
    update-alternatives --install /usr/bin/keytool keytool /usr/lib/jvm/*/bin/keytool 1 && \
    update-alternatives --set javac /usr/lib/jvm/*/bin/javac                            && \
    update-alternatives --set java /usr/lib/jvm/*/bin/java                              && \
    update-alternatives --set keytool /usr/lib/jvm/*/bin/keytool                        && \
    echo 'export JAVA_HOME='$(find /usr/lib/jvm/* -maxdepth 0 -type d) > /etc/profile.d/java.sh && \
    echo 'export JAVA_HOME='$(find /usr/lib/jvm/* -maxdepth 0 -type d) > /root/.bashrc
RUN rm -f /tmp/java.tgz

#
# Supervisord installation
#
RUN apt-get install -y supervisor
RUN cp /etc/supervisor/supervisord.conf /etc/supervisor/supervisord.conf.orig
ADD ./supervisord.conf /etc/supervisor/supervisord.conf

# standard volumes
VOLUME ["/logs"]

CMD /bin/bash
