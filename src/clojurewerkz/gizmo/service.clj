(ns ^{:doc "Registerable services for flexible architectures" }
  clojurewerkz.gizmo.service)

(defonce ^:dynamic
  ^{:doc "True by default.  If set to false, no services functions will
   be registered defservice.  Use this to skip service creation
    when compiling or loading code for testing purposes, usually
   services shouldn't be started then." }
  *load-services* true)

(defprotocol IService
  (start! [_] [_ config-override])
  (stop! [_])
  (alive? [_])
  (config [_])
  (thread [_]))

(defn empty-fn [& more])

(defn start-thread
  [f conf]
  (let [t (Thread. ^Runnable #(f conf))]
    (.start t)
    t))

(defn default-alive-fn
  [^Thread t]
  (when t
    (.isAlive t)))

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
       (let [start-thread# (atom nil)]
         (reify IService
           (start! [this#]
             (start! this# ~config))

           (start! [this# conf#]
             (when (alive? this#)
               (throw (RuntimeException. "Can't start service that was already started.")))
             (reset! start-thread#
                     (if (fn? conf#)
                       (start-thread ~start (conf#))
                       (start-thread ~start conf#))))

           (alive? [_]
             (when start-thread#
               (~alive (deref start-thread#))))

           (config [_]
             (let [conf# ~config]
               (if (fn? conf#)
                 (conf#)
                 conf#)))

           (stop! [this#]
             (when (not (alive? this#))
               (throw (RuntimeException. "Can't stop service that hasn't been started.")))
             (~stop))

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
