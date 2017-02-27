# Scribe

A solution to write and read Clojure data in various formats.

## Documentation
For more information about Scribe, it's design and protocols, please
read [the documentation](./doc/intro.md)

## Usage

```clojure
(ns com.test.my
  (:require [samsara.scribe.core :refer :all]
            [samsara.scribe.protocol :as scribe]))

(def configuration {:type :edn})

(def input {:id "some-uuid", :number 123, :persons #{"John" "Paul" "Lara"}})

(def serializer (scribe configuration))

(let [data (scribe/write serializer input)]
  (pr-str (scribe/read serializer data)))
```

## License

Copyright © 2015-2017 Samsara's Authors

Distributed under the Apache License v 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
