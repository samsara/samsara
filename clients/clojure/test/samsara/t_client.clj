(ns samsara.t-client
  (:use midje.sweet)
  (:use org.httpkit.fake)
  (:require [samsara.client :refer :all]
            [clojure.test :refer :all]
            [org.httpkit.client :as http]))

(defmacro fake-http
  ([status f]
   `(with-fake-http [{:url (str ((get-samsara-config) :url) "/events")
                      :method :post}
                     {:status ~status
                      :body ""}]
      (~f))))

;;(fake-http 200 (fn [] @(http/post (str ((get-samsara-config) :url) "/events"))))
;;(macroexpand '(fake-http 200 (fn [] @(http/get (str ((get-samsara-config) :url) "/events")))))

(defn get-events-url []
  (str ((get-samsara-config) :url) "/events"))

(defn event-with-name [event-name]
  "Returns an event with the given event-name"
  {:eventName event-name})

(defn get-events-in-buffer []
  "Returns eventNames of the current events in buffer"
  (map #(:eventName %1) (#'samsara.ring-buffer/items (deref (deref #'samsara.client/!buffer!)))))

(def record-event-with-name (comp record-event event-with-name))

;;set publish interval to a large value so that it will get out of way
(set-config! {:publish-interval 3600 :max-buffer-size 5})

(facts "publish-events validates events and sends them. Returns nil when successful and Exception on failure."
       (let [evt1 {:eventName "app.opened" :appName "Samsara"
                   :timestamp (System/currentTimeMillis)
                   :sourceId "d1"}
             evt2 {:eventName "app.closed" :appName "Samsara"
                   :timestamp (System/currentTimeMillis)
                   :sourceId "d1"}]

         (fake-http 202 (fn [] (publish-events [{:appName "Samsara"}])))
         => (throws IllegalArgumentException)

         (fake-http 202 (fn [] (publish-events [evt1])))
         => nil

         ;;Multiple events with mix of good and bad events.
         (fake-http 202 (fn [] (publish-events [evt1 {}])))
         => (throws IllegalArgumentException)

         ;;Multiple events all good
         (fake-http 202 (fn [] (publish-events [evt1 evt2])))
         => nil

         (fake-http 500 (fn [] (publish-events [evt2])))
         => (throws Exception)

         ))



(fact "enrich-event adds event headers, timestamp and sourceId fields to the event"
      (let [e {:eventName "AppOpened"}
            h (set-event-headers! {:version 5 :channel "paid"})]
        (keys (#'samsara.client/enrich-event e)) => (contains [:timestamp :eventName :sourceId :version :channel] :in-any-order)
        )
      )


(fact "send-events adds the X-Samsara-publishedTimestamp header to the request"
      (with-fake-http [{:url (get-events-url) :method :post}
                       (fn [orig-fn opts callback]
                         (if (contains? (opts :headers) "X-Samsara-publishedTimestamp")
                           {:status 202}
                           {:status 500}))]
        (#'samsara.client/send-events [{:eventName "AppOpened"}]) => nil))

(fact "calling record-event buffers the event in a ring buffer and periodically flushes it"
      ;;flush the buffer to avoid unintended residue
      (record-event-with-name "One")
      (prn (get-events-in-buffer))
      (fake-http 202 (fn [] (#'samsara.client/flush-buffer)))
      ;;sleep for a while to let flush finish.
      (Thread/sleep 500)
      (fact "record-event buffers events"
            ;;record 5 events and test buffer
            (doall (map #(record-event-with-name %1) ["One" "Two" "Three" "Four" "Five"]))
            (get-events-in-buffer) => (contains ["One" "Two" "Three" "Four" "Five"] :in-any-order))

      (fact "adding an event when the buffer is full overwrites the oldest event"
            ;;record sixth event and test buffer
            (record-event-with-name "Six")
            (get-events-in-buffer) => (contains ["Two" "Three" "Four" "Five" "Six"] :in-any-order))

      ;;successfully flush buffer while adding 2 more events
      (fact "events successfully flushed are removed while oldest events are replaced by new ones"
            (with-fake-http [{:url (get-events-url) :method :post}
                             (fn [orig-fn opts callback]
                               ;;Add 2 elements here
                               (record-event-with-name "Seven")
                               (record-event-with-name "Eight")
                               ;;TODO midje doesnt check here. Why ?
                               (get-events-in-buffer) => (contains ["Eight" "Seven" "Six" "Five" "Four"] :in-any-order)
                               {:status 202})]
              (#'samsara.client/flush-buffer)
              (get-events-in-buffer) => (contains ["Seven" "Eight"] :in-any-order)
              ))

      ;;Add four more events and simulate flush failure
      (fact "events are retained when flush fails while oldest events are replaced by new ones"
            (with-fake-http [{:url (get-events-url) :method :post}
                             (fn [orig-fn opts callback]
                               ;;Add 2 elements here
                               (prn (get-events-in-buffer))

                               (record-event-with-name "Nine")
                               (prn (get-events-in-buffer))

                               (record-event-with-name "Ten")
                               (prn (get-events-in-buffer))

                               (record-event-with-name "Eleven")
                               (prn (get-events-in-buffer))

                               (record-event-with-name "Twelve")

                               (prn (get-events-in-buffer))
                               ;;TODO midje doesnt check here. Why ?
                               ;;(get-events-in-buffer) => (contains ["Twelve" "Eleven" "Ten" "Nine" "Eight"] :in-any-order)
                               {:status 500})]
              (#'samsara.client/flush-buffer)
              (get-events-in-buffer) => (contains ["Twelve" "Eleven" "Ten" "Nine" "Eight"] :in-any-order)
              )))

;(record-event-with-name "Hello")



;(!init!)
