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
from collections import deque
import logging
from threading import Thread, Event

logging.basicConfig(level=logging.DEBUG)


def seconds_to_millis(s):
    return int(s * 1000)


def test_event():
    return {
        "timestamp": seconds_to_millis(time.time()),
        "sourceId": "3aw4sedrtcyvgbuhjkn",
        "eventName": "user.item.added",
        "page": "orders",
        "item": "sku-1234"
    }


class InvalidEvent(Exception):
    pass


class InvalidArguement(Exception):
    pass


class InvalidConfiguration(Exception):
    pass


class COMPRESSION(object):
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


event_validator = Validator(
    schema={
        'sourceId': {'required': True, 'type': 'string'},
        'timestamp': {'required': True, 'type': 'integer', 'min': 0},
        'eventName': {'required': True, 'type': 'string'}
    }, allow_unknown=True)


def validate_events(events):
    if not isinstance(events, list):
        events = [events]
    for e in events:
        if not event_validator.validate(e):
            raise InvalidEvent(
                "Invalid event found, errors:\n{}"
                .format(event_validator.errors))
    return events


def publish_events(url, events,
                   send_timeout=DEFAULT_CONFIG['send_timeout'],
                   compression=DEFAULT_CONFIG['compression']):
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
    )


class IntervalTimer(Thread):
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


class SamasaraClient(object):
    def __init__(self, **config):
        # init client with config
        self._init_config(dict(DEFAULT_CONFIG, **config))
        if self.config['start_publishing_thread']:
            self.start_consuming()

    def start_consuming(self):
        self.timer = self.get_timer(self.config['publish_interval'])
        logging.debug(
            'Starting publishing thread at an {}sec interval'
            .format(self.config['publish_interval']))
        self.timer.start()

    def stop_consuming(self):
        self.timer.stop()

    def _init_config(self, config):
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

        self.config = config

    def _flush_buffer(self):
        self.publish_events(
            [self.buffer.popleft() for _ in range(len(self.buffer))]
        )

    def _flush_buffer_if_ready(self):
        if len(self.buffer) >= self.config['min_buffer_size']:
            self._flush_buffer()

    def get_timer(self, interval):
        return IntervalTimer(interval, self._flush_buffer_if_ready)

    def record_event(self, event):
        if not event.get('sourceId'):
            event['sourceId'] = self.config['sourceId']
        if not event.get('timestamp'):
            event['timestamp'] = seconds_to_millis(time.time())

        validate_events(event)
        self.buffer.append(event)

    def publish_events(self, events):
        count = len(events)
        res = publish_events(self.config['url'], events,
                             send_timeout=self.config['send_timeout'],
                             compression=self.config['compression'])
        if res.ok:
            logging.debug(
                'Successfully submitted {} events'
                .format(count))
        else:
            logging.error(
                '{} events were not successfully submitted'
                .format(count))
