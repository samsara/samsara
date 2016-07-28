# -*- coding: utf-8 -*-
"""
    Samsara
    ~~~~~~~

    Samsara service package
"""

from setuptools import setup, find_packages


def get_long_description():
        with open('../../docs/clients/python-client.md') as f:
            return f.read()


def get_version():
        with open('../../samsara.version') as f:
            return f.read()

setup(
    name='samsara_sdk',
    version=get_version(),
    url='https://github.com/samsara/samsara',
    author='Samsara Developers',
    author_email='samsara.systems+info@gmail.com',
    description="A Python Client for Samsaza's Ingestion-API",
    long_description=get_long_description(),
    packages=find_packages(),
    zip_safe=False,
    include_package_data=True,
    platforms='any',
    tests_require=find_packages(include=['*-dev'])
)
