FROM anapsix/alpine-java:jdk8

MAINTAINER Samsara's team (https://github.com/samsara/samsara/docker-images)

#
# Common packages
#
RUN apk add --update bash tar ca-certificates wget curl bzip2 \
        less vim netcat-openbsd zip iproute2 perl \
        libstdc++ libc6-compat

#
# Supervisord installation
#
RUN apk add supervisor && \
    mkdir -p /etc/supervisor/conf.d && \
    mv /etc/supervisord.conf /etc/supervisor/supervisord.conf.orig
ADD ./supervisord.conf /etc/supervisor/supervisord.conf
RUN ln -s /etc/supervisor/supervisord.conf /etc/supervisord.conf

#
# Installing jq JSON processor
#
RUN apk add jq

#
# Installing synapse container linker
#
RUN wget --progress=dot:mega --no-check-certificate \
    https://www.dropbox.com/s/bpmi0v56ueyx2ag/synapse?dl=0 \
    -O /usr/local/bin/synapse && \
    chmod +x /usr/local/bin/synapse

#
# setting the terminal to xterm
#
ENV TERM xterm

# standard volumes
VOLUME ["/logs"]

CMD /bin/bash
