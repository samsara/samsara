# -*- coding: utf-8 -*-
"""
    Samsara
    ~~~~~~~

    Samsara service package
"""

from setuptools import setup, find_packages


setup(
    name='samsara_sdk',
    version='',
    url='https://github.com/samsara/samsara',
    author='Samsara Developers',
    author_email='samsara.systems+info@gmail.com',
    description="A Python Client for Samsaza's Ingestion-API",
    long_description="""

# Install and use

to install use:

    pip install samsara_sdk

to use please refer to the documentation at this website:

    http://samsara-analytics.io/docs/clients/python-client/


""",
    packages=find_packages(),
    include_package_data=True,
    platforms='any',
    keywords=['analytics', 'client', 'samsara'],
    tests_require=find_packages(include=['*-dev'])
)
