(ns ^{:doc "Registerable services for flexible architectures" }
  clojurewerkz.gizmo.service)

(defonce ^:dynamic
  ^{:doc "True by default.  If set to false, no service will be registered with defservice.
    Use this to skip service creation when compiling or loading code for testing purposes,
    usually services shouldn't be started then." }
  *load-services* true)

(defprotocol IService
  (start! [service] [service config-override])
  (stop! [service])
  (alive? [service])
  (config [service])
  (state [service])
  (reset-state [service new-state])
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
      you can use for the service.
     * `alive` by default is checking wether the thread that `start` function ran in is
      still alive. You can override default behavior to provide custom service
      availability check.
     * `stop` controls stopping the service. Perform all operations to gracefully
      shutdown the service here, and stop the thread where service was initially
      executed.
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
  []
  (->> (all-ns)
       (map #(vals (ns-interns %)))
       flatten
       (filter #(:service (meta %)))
       (map #(vector (keyword (:name (meta %))) (var-get %)))
       (into {})))

(defn start-all!
  []
  (doseq [service (all-services)]
    (start! service)))

(defn stop-all!
  []
  (doseq [service (all-services)]
    (stop! service)))
