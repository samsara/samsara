(ns samsara.qanal.coordinator.coordinator-protocol)

(defprotocol Coordinator

  (start [this])
  (stop [this]))
