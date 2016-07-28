# -*- coding: utf-8 -*-
"""
    samsara_sdk.client
    ~~~~~~~~~~~~~~~~~~

    Samsara SDK client
    TODO: support send-client-stats
"""
from urllib.parse import urljoin
from threading import RLock
import logging
from .helpers import (
    millis_to_seconds,
    InvalidConfiguration,
    IntervalTimer,
    validate_events,
    publish,
    COMPRESSION_HANDLERS,
    MonotonicDeque,
    current_time_millis
)

from .constants import (
    DEFAULT_CONFIG, API_PATH,
    PUBLISHED_TIMESTAMP_HEADER
)


class SamsaraClient(object):
    """
    A client for ingesting events into Samsara

    Configuration:
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

        # network timeout for send operations
        # in seconds
        # default 30s
        "send_timeout": 30000,

        # whether of not the payload should be compressed
        # allowed values {"gzip", "none"}
        "compression": "gzip"

        TODO: support this:
        # add samsara client statistics events
        # this helps you to understand whether the
        # buffer size and publish-intervals are
        # adequately configured.
        # "send_client_stats": True

    """
    def __init__(self, **config):
        """
        Initialize the Samsara Client with kwargs passed in
        merged with DEFAULT_CONFIG.
        """
        self._init_config(config)
        self._lock = RLock()
        self._buffer = MonotonicDeque(
            maxlen=self.config['max_buffer_size'])
        self.publisher = None
        if self.config['start_publishing_thread']:
            self.start_consuming()

    def _init_config(self, config):
        """
        Validate and sanitize client configuration
        """
        send_timeout_secs, publish_interval_secs = [
            millis_to_seconds(config.get(key) or DEFAULT_CONFIG[key])
            for key in ['send_timeout', 'publish_interval']]
        self.config = dict(
            DEFAULT_CONFIG,
            send_timeout_secs=send_timeout_secs,
            publish_interval_secs=publish_interval_secs,
            **config)

        try:
            self.config['endpoint'] = urljoin(
                self.config['url'], API_PATH)
        except KeyError:
            raise InvalidConfiguration(
                "Missing Samsara's ingestion api endpoint url.")
        try:
            compression = self.config['compression']
            self.compress = COMPRESSION_HANDLERS[compression]
        except KeyError:
            raise InvalidConfiguration(
                "Specified compression, {}, must be in {}"
                .format(compression, COMPRESSION_HANDLERS.keys()))

        if self.config['publish_interval'] < 0:
            raise InvalidConfiguration(
                "Publish interval needs to be above 0, not {}"
                .format(config['publish_interval']))

        if self.config['min_buffer_size'] > self.config['max_buffer_size'] \
           or self.config['min_buffer_size'] == 0:
            self.config['min_buffer_size'] = 1

    def _enrich_event(self, event):
        if not event.get('sourceId'):
            event['sourceId'] = self.config['sourceId']
        if not event.get('timestamp'):
            event['timestamp'] = current_time_millis()
        return event

    def record_event(self, **event):
        """
        Enriches the event if necessary with source/timestamp metadata,
        validates it and adds it to the end of the buffer
        """
        self._enrich_event(event)
        validate_events(event)
        self._buffer.enqueue(event)

    def publish_events(self, events):
        """
        Publishes valid `events` to `url` using `compression`'s format
        with a timeout of `send_timeout`
        """
        success = publish(self.config['endpoint'],
                          self.compress(list(events)),
                          self._form_headers(),
                          self.config['send_timeout_secs'])
        return success

    def _form_headers(self):
        return {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Content-Encoding": self.config['compression'] or "identity",
            PUBLISHED_TIMESTAMP_HEADER: str(current_time_millis())
        }

    def start_consuming(self):
        """
        Stops existing publisher thread and
        initializes a thread publish buffered at
        config["publish_interval_secs"]
        """
        if self.publisher:
            self.stop_consuming()
        self.publisher = self._get_timer(
            self.config['publish_interval_secs'])

        logging.debug(
            'Starting publishing thread at a(n) {}ms interval'
            .format(self.config['publish_interval']))
        self.publisher.start()

    def stop_consuming(self):
        """Stops existing the publisher thread"""
        if self.publisher:
            self.publisher.stop()

    def _get_timer(self, interval):
        """
        helper function to construct buffer flushing interval timer thread obj
        """
        return IntervalTimer(interval, self._flush_buffer_if_ready)

    def _flush_buffer(self):
        """
        Grabs events from the buffer and attempts to publish them
        if the events were not successfully published
        it requeues them
        """
        # popleft is atomic and allows the local thread with safe
        # access to the elements
        with self._lock:
            events = self._buffer
            last_id, count = self._buffer.last_event_id, len(events)

        success = self.publish_events(events)
        if success:
            logging.debug(
                'Successfully submitted {} events'
                .format(count))
            self._buffer.drop_until(last_id)
        else:
            logging.error(
                '{} events were not successfully submitted'
                .format(count))

    def _buffer_is_ready(self):
        return len(self._buffer) >= self.config['min_buffer_size']

    def _flush_buffer_if_ready(self):
        """Flushes buffer if it's greater than config['min_buffer_size']"""
        if self._buffer_is_ready():
            self._flush_buffer()
