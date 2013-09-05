(ns clojurewerkz.gizmo.core
  (:require [bultitude.core :as bultitude]))

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
