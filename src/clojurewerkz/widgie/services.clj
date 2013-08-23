(ns ^{:doc "Registerable services for flexible architectures" }
  clojurewerkz.widgie.services)

(defonce ^:dynamic
  ^{:doc "True by default.  If set to false, no services functions will
   be registered defservice.  Use this to skip service creation
    when compiling or loading code for testing purposes, usually
   services shouldn't be started then." }
  *load-services* true)

(defprotocol IService
  (start! [_])
  (stop! [_])
  (config [_])
  (thread [_]))

(defn empty-fn [& more])

(defmacro defservice
  {:added "1.1"}
  [name & {:keys [start stop config] :or {start empty-fn
                                          stop empty-fn
                                          config empty-fn} :as opts}]
  (when *load-services*
    `(def ~(vary-meta name assoc :service true :opts opts)
       (reify IService
         (start! [_#]
           (let [conf# ~config]
             (if (fn? conf#)
               (~start (conf#))
               (~start conf#))))

         (config [_#]
           (let [conf# ~config]
             (if (fn? conf#)
               (~start (conf#))
               (~start conf#))))

         (stop! [_#]
           (~stop))))))

(defn- start*
  [f]
  (let [t (Thread. ^Runnable f)]
    (.start t)
    t))

(defn make-default-service
  [opts {:keys [start stop pause]}]
  (let [thread (Thread. ^Runnable #(start opts))]
    (reify IService
      (start! [_]
        (.start thread))

      (stop! [_]
        (stop opts)
        (.stop thread))

      (config [_] opts)
      (thread [_] thread))))

(defn all-services
  []
  (->> (all-ns)
       (map #(vals (ns-interns %)))
       flatten
       (filter #(:service (meta %)))
       (map var-get)))

(defn start-all!
  []
  (doseq [service (all-services)]
    (start! service)))

(defn stop-all!
  []
  (doseq [service (all-services)]
    (stop! service)))


(defservice testserv
  :config 123
  :start (fn [cfg] (println cfg))
  )
