(ns clojurewerkz.gizmo.core
  "Gizmo core, set of functions for the application to start, based on Gizmo conventions for
   location of widgets, snippets, handlers and services."
  (:require [net.cgrand.reload :as enlive-reload]
            [bultitude.core :as bultitude]))

(defn- require-all
  [prefix]
  (doseq [ns (bultitude/namespaces-on-classpath :prefix prefix)]
    (require ns)))

(defn require-widgets
  "Loads all the namespaces containing widgets"
  [^String app]
  (require-all (str app ".widgets")))

(defn require-snippets
  "Loads all the namespaces containing snippets"
  [^String app]
  (require-all (str app ".snippets")))

(defn require-handlers
  "Loads all the namespaces containing handlers"
  [^String app]
  (require-all (str app ".handlers")))

(defn require-services
  "Loads all the namespaces containing services"
  [^String app]
  (require-all (str app ".services")))

(defn register-snippet-reload
  "Registers snippet reloading in Enlive"
  [name]
  (doseq [ns (bultitude/namespaces-on-classpath :prefix (str name ".snippets"))]
    (enlive-reload/auto-reload (find-ns ns))))
