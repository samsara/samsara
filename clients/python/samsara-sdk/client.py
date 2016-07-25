# -*- coding: utf-8 -*-
"""
    samsara-sdk.client
    ~~~~~~~~~~~~~~~~~~

    Samsara SDK client
"""

from cerberus import Validator
import gzip
import json
import time
import requests


class InvalidEvent(Exception):
    pass


class InvalidArguement(Exception):
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
        'source_id': {'required': True, 'type': 'string'},
        'timestamp': {'required': True, 'type': 'integer', 'min': 0},
        'event_name': {'required': True, 'type': 'string'}
    }, allow_unknown=True)


def validate_events(events):
    for e in events:
        if not event_validator.validate(e):
            raise InvalidEvent(
                "Invalid event found, errors:\n{}"
                .format(event_validator.errors))
        yield e


def publish_events(url, events,
                   send_timeout=DEFAULT_CONFIG['send_timeout'],
                   compression=DEFAULT_CONFIG['compression']):
    shared_headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "Content-Encoding": compression or "identity",

    }
    compress = COMPRESSION.get_compressor(compression)
    for e in validate_events(events):
        headers = dict({
            PUBLISHED_TIMESTAMP: str(time.time())
        }, **shared_headers)
        requests.post(
            url + "/v1/events",
            headers=headers,
            data=compress(e),
            timeout=send_timeout
        )


class SamasaraClient(object):
    def __init__(url, source_id=None, **DEFAULT_CONFIG):
        # init client with config
        pass

    def record_event(event):
        # add to buffer, if buffer is at capacity, flush then add
        pass
