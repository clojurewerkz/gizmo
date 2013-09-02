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
  [t]
  (.isAlive t))

(defmacro defservice
  "Defines a new serivice with given `name`.

   "
  [name & {:keys [start stop config alive] :or {start empty-fn
                                                stop empty-fn
                                                config empty-fn
                                                alive default-alive-fn} :as opts}]
  (when *load-services*
    `(def ~(vary-meta name assoc :service true :opts opts)
       (let [start-thread# (atom nil)]
         (reify IService
           (start! [_]
             (let [conf# ~config]
               (reset! start-thread#
                       (if (fn? conf#)
                         (start-thread ~start (conf#))
                         (start-thread ~start conf#)))))

           (start! [_ conf#]
             (reset! start-thread#
                     (if (fn? conf#)
                       (start-thread ~start (conf#))
                       (start-thread ~start conf#))))

           (alive? [_]
             (~alive (deref start-thread#)))

           (config [_]
             (let [conf# ~config]
               (if (fn? conf#)
                 (conf#)
                 conf#)))

           (stop! [_#]
             (~stop)))))))

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
