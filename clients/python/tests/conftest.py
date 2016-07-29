# -*- coding: utf-8 -*-
"""
    samsara_sdk.client
    ~~~~~~~~~~~~~~~~~~

    Samsara SDK client
    TODO: support send-client-stats
"""
import pytest


@pytest.fixture
def fake_data():
    registers = [{"eventName": "user.registration"} for _ in range(30)]
    gains = [{"eventName": "user.skill.gain"} for _ in range(30)]
    return registers, gains
