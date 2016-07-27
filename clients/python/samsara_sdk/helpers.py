# -*- coding: utf-8 -*-
"""
    samsara_sdk.helpers
    ~~~~~~~~~~~~~~~~~~

    Samsara SDK client helpers
"""
import logging
from threading import Thread, Event
from types import GeneratorType
from cerberus import Validator
import gzip
import json
import requests
from collections import deque

from .constants import DEFAULT_CONFIG

logging.basicConfig(level=logging.DEBUG)


def seconds_to_millis(s):
    return int(s * 1000)


def millis_to_seconds(m):
    return int(m / 1000)


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


COMPRESSION_HANDLERS = {
    None: json.dumps,
    'gzip': lambda x: gzip.compress(bytes(json.dumps(x), 'utf-8'))
}

EVENT_VALIDATOR = Validator(
    schema={
        'sourceId': {'required': True, 'type': 'string'},
        'timestamp': {'required': True, 'type': 'integer', 'min': 0},
        'eventName': {'required': True, 'type': 'string'}
    }, allow_unknown=True)


def validate_events(events):
    """Validates events with `EVENT_VALIDATOR`"""
    accepted_iters = (list, GeneratorType, tuple)
    if not isinstance(events, accepted_iters):
        events = [events]
    for e in events:
        if not EVENT_VALIDATOR.validate(e):
            raise InvalidEvent(
                "Invalid event found, errors:\n{}"
                .format(EVENT_VALIDATOR.errors))
    return events


def publish(url, events, headers=None,
            send_timeout=millis_to_seconds(
                DEFAULT_CONFIG['send_timeout']
            )):
    req = requests.post(
        url,
        headers=headers or {},
        data=events,
        timeout=send_timeout
    )
    if not req.ok:
        raise
    return req.ok


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


class MonotonicDeque(deque):
    """
    A limited deque with an monotonic counter
    """

    def __init__(self, maxlen):
        iterable = ()
        deque.__init__(self, iterable, maxlen)
        self.last_event_id = 0

    def enqueue(self, el):
        el['__id__'] = self.last_event_id
        self.last_event_id += 1
        self.append(el)

    def drop_until(self, last_id):
        while self[0]['__id__'] <= last_id:
            self.popleft()
            if not self:
                break
