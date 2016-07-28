# -*- coding: utf-8 -*-
"""
    tests.test_client
    ~~~~~~~~~~~~~~~~~~

    Tests Samsara SDK client
"""
import pytest
from samsara_sdk.client import SamsaraClient
from samsara_sdk.helpers import IntervalTimer
import threading
from os import environ


def interval_threads():
    return [
        x for x in threading.enumerate()
        if isinstance(x, IntervalTimer)]


@pytest.mark.integration
def test_client(fake_data):
    assert not interval_threads()
    api_netloc = environ.get("INGESTION_API_NETLOC")
    client = SamsaraClient(
        url=api_netloc,
        sourceId="ed303d68-53a6-11e6-8e25-3f5e0ad08c59",
        publish_interval=1000,
        min_buffer_size=10,
        start_publishing_thread=False
    )
    assert not interval_threads()
    registers_and_gains = fake_data[0] + fake_data[1]
    for e in registers_and_gains:
        client.record_event(**e)

    assert client._buffer
    assert client._buffer_is_ready()
    client._flush_buffer_if_ready()
    assert not client._buffer

    client.publish_events([client._enrich_event(x) for x in client._buffer])
    assert not client._buffer
