(ns clojurewerkz.gizmo.services.nrepl-service
  ^{:doc "Example service implementation, nrepl service" }
  (:use [clojurewerkz.gizmo.service])
  (:require [clojure.tools.nrepl.server :as nrepl]))

(declare nrepl-server)

(defservice nrepl-service
  :config [:port 4556]
  :alive (fn [_]
           (and (bound? (var nrepl-server))
                (not (.isClosed (.server-socket nrepl-server)))))
  :stop (fn []
          (nrepl/stop-server nrepl-server))
  :start (fn [cfg]
           (def nrepl-server (apply nrepl/start-server cfg))))
