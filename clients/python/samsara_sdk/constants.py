# -*- coding: utf-8 -*-
"""
    samsara_sdk.constants
    ~~~~~~~~~~~~~~~~~~~~~

    Samsara SDK client constants
"""
# Samara specific HTTP Header
PUBLISHED_TIMESTAMP_HEADER = "X-Samsara-publishedTimestamp"

API_PATH = '/v1/events'
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

    # network timeout for send operations
    # in milliseconds
    # default 30s
    "send_timeout": 30000,

    # whether of not the payload should be compressed
    # allowed values "gzip" "none"
    "compression": "gzip"

    # add samsara client statistics events
    # this helps you to understand whether the
    # buffer size and publish-intervals are
    # adequately configured.
    # "send_client_stats": True
}
