(ns clojurewerkz.gizmo.services.nrepl-service
  ^{:doc "Example service implementation, nrepl service" }
  (:use [clojurewerkz.gizmo.service])
  (:require [clojure.tools.nrepl.server :as nrepl]))

(defservice nrepl-service
  :config [:port 4558]
  :alive (fn [service]
           (let [server (state service)]
             (and server
                  (not (.isClosed (.server-socket server))))))
  :stop (fn [service]
          (nrepl/stop-server (state service)))
  :start (fn [service]
           (reset-state service
                        (apply nrepl/start-server (config service)))))
