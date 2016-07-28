# samara-sdk

A Python 3 client sdk for Samsara.

## Usage

To see how to use this client please [read the documentation](/docs/clients/python-client.md)

## Contributing

Follow the [Quick-Start guide](/docs/quick-start.md) to get Samsara & friends set up.

Install requirements in a [virtualenv](https://virtualenv.pypa.io/en/stable/):

```
$
$ pip install -r requirements.txt
$ pip install -r requirements-dev.txt
```

To run integration tests:

```
$ export INGESTION_API_NETLOC={network location of ingestion api to be tested}
# If you're using Samsara on Docker, http://$(docker-machine ip samsara-vm):9000 works
$ py.test
```

### TODO

- [ ] support `"send_client_stats"` option

## License

Copyright © 2016 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
