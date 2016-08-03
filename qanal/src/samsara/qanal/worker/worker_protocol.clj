(ns samsara.qanal.worker.worker-protocol)

(defprotocol Worker

  (all-jobs [this] "Returns a sequence of jobs, some of which will be passed to start fn")

  (assigned-jobs [this] "Returns a sequence of jobs that this worker has been assigned
                         via start fn")

  (start [this jobs] "Starts on the assigned jobs")

  (stop [this] "Stops any ongoing jobs"))
