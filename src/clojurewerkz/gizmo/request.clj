(ns clojurewerkz.gizmo.request
  (:require [net.cgrand.enlive-html :as html]))

(declare ^{:dynamic true} *request*)

(defn request
  []
  *request*)

(defmacro with-request [request-hash & body]
  `(binding [*request* ~request-hash]
     ~@body))

(defn xhr?
  ([]
     (xhr? (request)))
  ([request]
     (= "XMLHttpRequest"
        (get-in request [:headers "x-requested-with"]))))
