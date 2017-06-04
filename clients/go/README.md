# client

Go client for Samsara.

## Usage

To see how to use this client please [read the documentation](/docs/clients/go-client.md)

## Contributing

Follow the [Quick-Start guide](/docs/quick-start.md) to get Samsara & friends set up.

To develop client on your machine:

```bash
$ git clone https://github.com/samsara/samsara.git
$ cd samsara/clients/go
$ # develop and make some code changes
$ go vet # to check code-sanity
$ go test -v # to run tests
$ go test --cover # to generate code coverage report
$ go fmt # always format your code before committing!
```

To install client on your machine:

```
$ go get github.com/samsara/samsara/clients/go
```

### TODO

- [ ] support `"SendClientStats"` option

## License

Copyright © 2017 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)