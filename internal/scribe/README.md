# Scribe

A solution to write and read Clojure data in various formats.

## Documentation
Scribe is a Clojure library designed to store and load data without
losing the semantic characteristic of certain types.

The Scribe protocol has two functions:

```clojure
 (write serializer data)  -> bytes
 (read  serializer bytes) -> data
```

Each function takes two arguments: particular serializer and data to
serialize/deserialize.

Serializers can be constructed via scribe multimethod:

```clojure
(scribe {:type :nippy :config :more})
```

Where `:type` is a serializer type, and `:config`, `:more` are
configuration parameters for a particular serializer.

## Serializers

Out of the box Scribe provides the following serializers:

* `:json` - [JSON](https://github.com/dakrone/cheshire)
* `:edn`  - [EDN](https://github.com/edn-format/edn)
* `:nippy` - [Nippy](https://github.com/ptaoussanis/nippy)
* `:fressian` - [Fressian](https://github.com/clojure/data.fressian)
* `:transit` - [Transit](https://github.com/cognitect/transit-clj)

## Usage

```clojure
(ns com.test.my
  (:require [samsara.scribe.core :refer [scribe]
            [samsara.scribe.protocol :as scribe]))

(def configuration {:type :edn})

(def input {:id "some-uuid", :number 123, :persons #{"John" "Paul" "Lara"}})

(def serializer (scribe configuration))

(let [data (scribe/write serializer input)]
  (pr-str (scribe/read serializer data)))
```

## License

Copyright © 2015-2017 Samsara's authors.

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
