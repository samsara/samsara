FROM ubuntu:15.10

MAINTAINER Samsara's team (https://github.com/samsara/samsara-docker-images)

RUN apt-get update
RUN apt-get install -y wget curl bzip2 less vim netcat zip


#
# JDK installation
#

RUN export SJDK7="http://download.oracle.com/otn-pub/java/jdk/7u79-b15/jdk-7u79-linux-x64.tar.gz" && \
    export SJDK8="http://download.oracle.com/otn-pub/java/jdk/8u71-b15/jdk-8u71-linux-x64.tar.gz" && \
    export SJDK=$SJDK7 && \
    wget --progress=dot:mega --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" $SJDK  -O /tmp/java.tgz && \
    mkdir -p /usr/lib/jvm && \
    tar zxvf /tmp/java.tgz -C /usr/lib/jvm && \
    update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/*/bin/javac 1       && \
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

#
# Installing jq JSON processor
#
RUN wget http://stedolan.github.io/jq/download/linux64/jq -O /usr/local/bin/jq && \
    chmod +x /usr/local/bin/jq

#
# setting the terminal to xterm
#
ENV TERM xterm

# standard volumes
VOLUME ["/logs"]

CMD /bin/bash
