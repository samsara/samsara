# -*- coding: utf-8 -*-
"""
    Samsara
    ~~~~~~~

    Samsara service package
"""

from setuptools import setup, find_packages


def get_long_description():
        with open('./info/python-client.md') as f:
            return f.read()


def get_version():
        with open('./info/samsara.version') as f:
            return f.read().split('-')[0]

setup(
    name='samsara_sdk',
    version=get_version(),
    url='https://github.com/samsara/samsara',
    author='Samsara Developers',
    author_email='samsara.systems+info@gmail.com',
    description="A Python Client for Samsaza's Ingestion-API",
    long_description=get_long_description(),
    packages=find_packages(),
    include_package_data=True,
    platforms='any',
    keywords=['analytics', 'client', 'samsara'],
    tests_require=find_packages(include=['*-dev'])
)
