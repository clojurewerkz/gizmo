(ns clojurewerkz.gizmo.core
  "Gizmo core, set of functions for the application to start, based on Gizmo conventions for
   location of widgets, snippets, handlers and services."
  (:require [bultitude.core :as bultitude]))

(defn- require-all
  [prefix]
  (doseq [ns (bultitude/namespaces-on-classpath :prefix prefix)]
    (require ns)))

(defn require-handlers
  "Loads all the namespaces containing handlers"
  [^String app]
  (require-all (str app ".handlers")))

(defn require-services
  "Loads all the namespaces containing services"
  [^String app]
  (require-all (str app ".services")))
