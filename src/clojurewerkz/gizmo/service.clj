(ns ^{:doc "Registerable services for flexible architectures. Services are usually defined in
a cooperative manner, you can check out UDP server for details on how to implement a cooperative
service. " }
  clojurewerkz.gizmo.service)

(defonce ^:dynamic
  ^{:doc "True by default.  If set to false, no service will be registered with defservice.
    Use this to skip service creation when compiling or loading code for testing purposes,
    usually services shouldn't be started then." }
  *load-services* true)

(defprotocol IService
  (start! [service] "Start service")
  (stop! [service] "Stop service")
  (alive? [service] "Checks wether service is still alive or no.")
  (config [service] "Returns service configuration")
  (state [service] "Get current service state.")
  (reset-state [service new-state] "Update service state by setting `new-state`.")
  (thread [service]))

(defn empty-fn [& more])

(defn start-thread
  [start-fn serivice]
  (let [t (Thread. ^Runnable #(start-fn serivice))]
    (.start t)
    t))

(defn default-alive-fn
  [service]
  (let [t (thread service)]
    (when t
      (.isAlive t))))

(defmacro defservice
  "Defines a new serivice with given `name`.

   You may specify `start`, `stop`, `config` and `alive` functions.

     * `start` is responsible for server start. It's called in a separate thread, that
      you can use for the service. It is usually a either long-running function that blocks
      thread execution and creates an internal endless loop, or fires up a timer or another
      control thread. Function receives `service` instance, that you can use
      to obtain and update service state. Stop function usually notifies `start` about
      the fact that service should be started in cooperative manner, through service
      state.

     * `alive` by default is checking wether the thread that `start` function ran in is
      still alive. You can override default behavior to provide custom service
      availability check. You can use service state in `start` and `stop` functions in
      to control execution flow.

     * `stop` controls stopping the service. Perform all operations to gracefully
      shutdown the service here, and stop the thread where service was initially
      executed. Make sure that stopping service shutdowns the thread it's been running in.

     * `config` function that retrieves configuration or configuration value that will
      be passed to `start` function when service starts.
  "
  [name & {:keys [start stop config alive] :or {start empty-fn
                                                stop empty-fn
                                                config empty-fn
                                                alive default-alive-fn} :as opts}]
  (when *load-services*
    `(def ~(vary-meta name assoc :service true :opts opts)
       (let [start-thread#  (atom nil)
             service-state# (atom nil)]
         (reify IService
           (start! [this#]
             (when (alive? this#)
               (throw (RuntimeException. "Can't start service that was already started.")))
             (reset! start-thread#
                     (start-thread ~start this#)))

           (alive? [this#]
             (~alive this#))

           (config [this#]
             (let [conf# ~config]
               (if (fn? conf#)
                 (conf#)
                 conf#)))

           (stop! [this#]
             (when (not (alive? this#))
               (throw (RuntimeException. "Can't stop service that hasn't been started.")))
             (~stop this#))

           (state [_]
             (deref service-state#))

           (reset-state [this# new-state#]
             (reset! service-state# new-state#))

           (thread [_]
             (deref start-thread#)))))))

(defn all-services
  "Returns all services registered in all namespaces"
  []
  (->> (all-ns)
       (map #(vals (ns-interns %)))
       flatten
       (filter #(:service (meta %)))
       (map #(vector (keyword (:name (meta %))) (var-get %)))
       (into {})))

(defn start-all!
  "Starts all services registered in all namespaces"
  []
  (doseq [[_ service] (all-services)]
    (start! service)))

(defn stop-all!
  "Stops all serivces registered in all namespaces"
  []
  (doseq [[_ service] (all-services)]
    (stop! service)))
