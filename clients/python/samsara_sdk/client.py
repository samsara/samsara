# -*- coding: utf-8 -*-
"""
    samsara_sdk.client
    ~~~~~~~~~~~~~~~~~~

    Samsara SDK client
    TODO: support send-client-stats
"""

from cerberus import Validator
import gzip
import json
import time
import requests
from collections import deque
import logging
from threading import Thread, Event

logging.basicConfig(level=logging.DEBUG)


def seconds_to_millis(s):
    return int(s * 1000)


class InvalidEvent(Exception):
    """
    An exception helper indicating the attempted
    submission of an invalid event
    """
    pass


class InvalidArguement(Exception):
    """
    An exception helper indicating the attempted
    invocation of a function with
    inappropriate Arguments
    """
    pass


class InvalidConfiguration(Exception):
    """
    An exception helper indicating the attempted
    construction of a Samsara Client
    with an inappropriate Configuration
    """
    pass


class COMPRESSION(object):
    """Compression protocol helper class"""
    GZIP = "gzip"

    def compress_none(s):
        return json.dumps(s)

    def compress_gzip(s):
        return gzip.compress(bytes(json.dumps(s), 'utf-8'))

    COMPRESS = {
        GZIP: compress_gzip,
        None: compress_none
    }

    def get_compressor(compression):
        """
        given a compress format attempts to return the compression function
        """
        if compression not in COMPRESSION.COMPRESS.keys():
            raise InvalidArguement(
                "Compression has to be one of {}"
                .format(COMPRESSION.COMPRESS.keys()))

        return COMPRESSION.COMPRESS.get(compression)

# Samara specific HTTP Header
PUBLISHED_TIMESTAMP = "X-Samsara-publishedTimestamp"

DEFAULT_CONFIG = {
    # a samsara ingestion api endpoint  "http://samsara.io/"
    # url  - REQUIRED

    # the identifier of the source of these events
    # source_id  - OPTIONAL only for record-event

    # whether to start the publishing thread.
    "start_publishing_thread": True,

    # how often should the events being sent to samsara
    # in seconds
    # default 30s
    "publish_interval": 30,

    # max size of the buffer, when buffer is full
    # older events are dropped.
    "max_buffer_size": 10000,

    # minimum number of events to that must be in the buffer
    # before attempting to publish them
    "min_buffer_size": 100,

    # network timeout for send operations (in seconds)
    # default 30s
    "send_timeout": 30,

    # whether of not the payload should be compressed
    # allowed values "gzip" "none"
    "compression": COMPRESSION.GZIP

    # add samsara client statistics events
    # this helps you to understand whether the
    # buffer size and publish-intervals are
    # adequately configured.
    # "send_client_stats": True
}


EVENT_VALIDATOR = Validator(
    schema={
        'sourceId': {'required': True, 'type': 'string'},
        'timestamp': {'required': True, 'type': 'integer', 'min': 0},
        'eventName': {'required': True, 'type': 'string'}
    }, allow_unknown=True)


def validate_events(events):
    """Validates events with `EVENT_VALIDATOR`"""
    if not isinstance(events, list):
        events = [events]
    for e in events:
        if not EVENT_VALIDATOR.validate(e):
            raise InvalidEvent(
                "Invalid event found, errors:\n{}"
                .format(EVENT_VALIDATOR.errors))
    return events


def publish_events(url, events,
                   send_timeout=DEFAULT_CONFIG['send_timeout'],
                   compression=DEFAULT_CONFIG['compression']):
    """
    Publishes valid `events` to `url` using `compression`'s format
    with a timeout of `send_timeout`
    """
    valid_events = validate_events(events)
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "Content-Encoding": compression or "identity",
        PUBLISHED_TIMESTAMP: str(seconds_to_millis(time.time()))
    }
    compress = COMPRESSION.get_compressor(compression)

    return requests.post(
        url + "/v1/events",
        headers=headers,
        data=compress(valid_events),
        timeout=send_timeout
    ).ok()


class IntervalTimer(Thread):
    """
    A Thread sublcass which calls a function
    at a certain inteval, until `stop`ed
    """
    def __init__(self, interval, func):
        Thread.__init__(self)
        self.interval = interval
        self.func = func
        self.stopped = Event()

    def run(self):
        while not self.stopped.wait(self.interval):
            self.func()

    def stop(self):
        self.stopped.set()


class SamsaraClient(object):
    def __init__(self, **config):
        """
        Initialized the Samsara Client with kwargs passed in
        merged with DEFAULT_CONFIG.


        Parameters:
        # a samsara ingestion api endpoint  "http://samsara.io/"
        # url  - REQUIRED

        # the identifier of the source of these events
        # source_id  - OPTIONAL only for record-event

        # whether to start the publishing thread.
        "start_publishing_thread": True,

        # how often should the events being sent to samsara
        # in seconds
        # default 30s
        "publish_interval": 30,

        # max size of the buffer, when buffer is full
        # older events are dropped.
        "max_buffer_size": 10000,

        # minimum number of events to that must be in the buffer
        # before attempting to publish them
        "min_buffer_size": 100,

        # network timeout for send operations (in seconds)
        # default 30s
        "send_timeout": 30,

        # whether of not the payload should be compressed
        # allowed values {"gzip", "none"}
        "compression": COMPRESSION.GZIP

        TODO: support this:
        # add samsara client statistics events
        # this helps you to understand whether the
        # buffer size and publish-intervals are
        # adequately configured.
        # "send_client_stats": True
        """
        self._init_config(dict(DEFAULT_CONFIG, **config))

        if self.config['start_publishing_thread']:
            self.start_consuming()

    def _init_config(self, config):
        """
        Validate and sanitize client configuration, initalize buffer/publisher
        """
        if not config['url']:
            raise InvalidConfiguration(
                "Missing Samsara's ingestion api endpoint url.")
        if config['publish_interval'] < 0:
            raise InvalidConfiguration(
                "Publish interval needs to be above 0, not {}"
                .format(config['publish_interval']))
        if config['min_buffer_size'] > config['max_buffer_size'] \
           or config['min_buffer_size'] == 0:
            config['min_buffer_size'] = 1

        self.buffer = deque(maxlen=config['max_buffer_size'])
        self.publisher = None
        self.config = config

    def start_consuming(self):
        """
        Stops existing publisher thread and
        initializes a thread publish buffered at config["publish_interval"]
        """
        if self.publisher:
            self.stop_consuming()
        self.publisher = self._get_timer(self.config['publish_interval'])
        logging.debug(
            'Starting publishing thread at an {}sec interval'
            .format(self.config['publish_interval']))
        self.publisher.start()

    def stop_consuming(self):
        """Stops existing the publisher thread"""
        if self.publisher:
            self.publisher.stop()

    def _flush_buffer(self):
        """
        Grabs events from the buffer and attempts to publish them
        if the events were not successfully published
        it requeues them
        """
        # popleft is atomic and allows the local thread with safe
        # access to the elements
        events = [self.buffer.popleft() for _ in range(len(self.buffer))]
        success = self.publish_events(events)
        count = len(events)
        if success:
            logging.debug(
                'Successfully submitted {} events'
                .format(count))
        else:
            logging.error(
                '{} events were not successfully submitted, requeueing'
                .format(count))
            # if we were unable to publish the events
            # we add them to buffer and hope to publish them
            # later
            for e in events:
                self.record_event(e)

    def _flush_buffer_if_ready(self):
        """Flushes buffer if it's greater than config['min_buffer_size']"""
        if len(self.buffer) >= self.config['min_buffer_size']:
            self._flush_buffer()

    def _get_timer(self, interval):
        """
        helper function to construct buffer flushing interval timer thread obj
        """
        return IntervalTimer(interval, self._flush_buffer_if_ready)

    def record_event(self, event):
        """
        Enriches the event if necessary with source/timestamp metadata,
        validates it and adds it to the end of the buffer
        """
        if not event.get('sourceId'):
            event['sourceId'] = self.config['sourceId']
        if not event.get('timestamp'):
            event['timestamp'] = seconds_to_millis(time.time())

        validate_events(event)
        self.buffer.append(event)

    def publish_events(self, events):
        """
        Wrapper around `publish_events` that uses the client's configuration.
        """
        success = publish_events(self.config['url'], events,
                                 send_timeout=self.config['send_timeout'],
                                 compression=self.config['compression'])
        return success
