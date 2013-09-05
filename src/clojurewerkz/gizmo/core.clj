(ns clojurewerkz.gizmo.core
  (:require [net.cgrand.reload :as enlive-reload]
            [bultitude.core :as bultitude]))

(defn- require-all
  [prefix]
  (doseq [ns (bultitude/namespaces-on-classpath :prefix prefix)]
    (require ns)))

(defn require-widgets
  [^String app]
  (require-all (str app ".widgets")))

(defn require-snippets
  [^String app]
  (require-all (str app ".snippets")))

(defn require-handlers
  [^String app]
  (require-all (str app ".handlers")))

(defn require-services
  [^String app]
  (require-all (str app ".services")))

(defn register-snippet-reload
  [name]
  (doseq [ns (bultitude/namespaces-on-classpath :prefix (str name ".snippets"))]
    (enlive-reload/auto-reload (find-ns ns))))
