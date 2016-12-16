# samara_sdk

Ruby client SDK for Samsara.

## Usage

To see how to use this client please [read the documentation](/docs/clients/ruby-client.md)

## Contributing

Follow the [Quick-Start guide](/docs/quick-start.md) to get Samsara & friends set up.

To hack client locally:

```bash
$ gem install bundler # (if not installed)
$ gem install rake # (if not installed)
$ git clone https://github.com/samsara/samsara.git
$ cd samsara/clients/ruby
$ rake rubocop # to check code-standards
$ rake test # to run tests
$ rake coverage # to generate code coverage report
```

To install client locally:

```bash
$ cd samsara/clients/ruby
$ rake build
```

### TODO

- [ ] support `"send_client_stats"` option

## License

Copyright © 2016 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
