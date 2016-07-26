# -*- coding: utf-8 -*-
"""
    samsara_sdk.client
    ~~~~~~~~~~~~~~~~~~

    Samsara SDK client
    TODO: use proper ring buffer
use from threading import Timer for interval thread execution
"""

from cerberus import Validator
import gzip
import json
import time
import requests
from collections import deque, Iterable


class InvalidEvent(Exception):
    pass


class InvalidArguement(Exception):
    pass


class InvalidConfiguration(Exception):
    pass


class COMPRESSION(object):
    GZIP = "gzip"

    @staticmethod
    def compress_none(s):
        return json.dumps(s)

    @staticmethod
    def compress_gzip(s):
        return gzip.compress(bytes(json.dumps(s), 'utf-8'))

    COMPRESS = {
        GZIP: compress_gzip,
        None: compress_none
    }

    @staticmethod
    def get_compressor(compression):
        if compression not in COMPRESSION.COMPRESS.keys():
            raise InvalidArguement(
                "Compression has to be one of {}"
                .format(COMPRESSION.COMPRESS.keys()))

        return COMPRESSION.COMPRESS.get(compression)


PUBLISHED_TIMESTAMP = "X-Samsara-publishedTimestamp"
DEFAULT_CONFIG = {
    # a samsara ingestion api endpoint  "http://samsara.io/"
    # url  - REQUIRED

    # the identifier of the source of these events
    # source_id  - OPTIONAL only for record-event

    # whether to start the publishing thread.
    "start_publishing_thread": True,

    # how often should the events being sent to samsara
    # in milliseconds
    # default 30s
    "publish_interval": 30000,

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


event_validator = Validator(
    schema={
        'sourceId': {'required': True, 'type': 'string'},
        'timestamp': {'required': True, 'type': 'integer', 'min': 0},
        'eventName': {'required': True, 'type': 'string'}
    }, allow_unknown=True)


def validate_events(events):
    if not isinstance(events, Iterable):
        events = [events]
    for e in events:
        if not event_validator.validate(e):
            raise InvalidEvent(
                "Invalid event found, errors:\n{}"
                .format(event_validator.errors))


def publish_events(url, events,
                   send_timeout=DEFAULT_CONFIG['send_timeout'],
                   compression=DEFAULT_CONFIG['compression']):
    validate_events(events)
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "Content-Encoding": compression or "identity",
        PUBLISHED_TIMESTAMP: str(time.time())
    }
    compress = COMPRESSION.get_compressor(compression)

    requests.post(
        url + "/v1/events",
        headers=headers,
        data=compress(events),
        timeout=send_timeout
    )


class SamasaraClient(object):
    def __init__(self, **config):
        # init client with config
        config = dict(DEFAULT_CONFIG, **config)

        if not config['url']:
            raise InvalidConfiguration(
                "Missing Samsara's ingestion api endpoint url.")
        if config['publish_interval'] < 0:
            raise InvalidConfiguration(
                "Publish interval needs to be above 0, not {}"
                .format(config['publish_interval']))
        if config['min-buffer-size'] > config['max-buffer-size'] \
           or config['min-buffer-size'] == 0:
            config['min-buffer-size'] = 1

        self.buffer = deque(maxlen=config['max-buffer-size'])

        self.config = config

    def record_event(self, event):
        # add to buffer, if buffer is at capacity, flush then add
        if not event.get('sourceId'):
            event['sourceId'] = self.config['sourceId']
        if not event.get('timestamp'):
            event['timestamp'] = time.time()
        validate_events(event)
        self.deque.append(event)

    def publish_events(self, events):
        publish_events(self.config['url'], events,
                       send_timeout=self.config['send_timeout'],
                       compression=self.config['compression'])
