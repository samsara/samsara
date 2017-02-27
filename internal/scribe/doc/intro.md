# Scribe

A Clojure library designed to store and load data without losing the semantic characteristic of certain types.

The Scribe protocol has two functions:

```clojure
 (write this data) -> bytes
 (read this bytes) -> data
```

Each function takes two arguments: particular serializer and data to serialize/deserialize.

Serializers can be constructed via scribe multimethod:

```clojure
(scribe {:type :nippy :config :more})
```

Where `:type` is a serializer type, and `:config`, `:more` are configuration parameters for a particular serializer.

## Serializers

Out of the box Scribe provides the following serializers:

* [JSON](https://github.com/dakrone/cheshire)
* [EDN](https://github.com/edn-format/edn)
* [Fressian](https://github.com/clojure/data.fressian)
* [Transit](https://github.com/cognitect/transit-clj)
* [Nippy](https://github.com/ptaoussanis/nippy)

## License

Copyright © 2015-2017 Samsara's Authors

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
