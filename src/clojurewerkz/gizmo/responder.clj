(ns clojurewerkz.gizmo.responder
  (:require [clojurewerkz.gizmo.widget :as widget]
            [clojurewerkz.gizmo.request :as request]
            [clojurewerkz.gizmo.utils.hash-utils :as hash-utils]

            [cheshire.core :as json]
            [net.cgrand.enlive-html :as html]))

(defn render
  [nodes]
  (apply str (html/emit* nodes)))

(defmulti respond-with (fn [response]
                         (assert (map? response)
                                 "Can't render response since it's not map. Please use handler returns in form of {:render :html ... }.")
                         (or (:render response) :json)))

(defmethod respond-with :nothing
  [env]
  {:body ""})

(defmethod respond-with :json
  [env]
  (let [response (json/generate-string (or (:response-hash env) {}))]
    {:headers (merge (:headers env)
                     {"content-type"  "application/json; charset=utf-8"
                      "content-length" (str (count response))})
     :status (or (:status env) 200)
     :body response}))

(defmethod respond-with :html
  [{:keys [widgets status headers layout] :as env}]
  (assert (> (count (widget/all-layouts)) 0) "Can't respond with :html without layouts given")
  (let [layout-template (if layout
                          (get (widget/all-layouts) layout)
                          (last (first (widget/all-layouts))))
        response        (request/with-request env
                          (render
                           (widget/interpolate-widgets
                            (widget/inject-core-widgets (layout-template)
                                                        (:widgets env))
                            env)))]
    {:headers (merge headers
                     {"content-type"  "text/html; charset=utf-8"
                      "content-length" (str (count response))})
     :status (or status 200)
     :body response}))

(defn wrap-responder
  [handler]
  (fn [env]
    (let [handler-env  (handler env)
          complete-env (hash-utils/deep-merge env handler-env)]
      (println "Handling uri: " (:uri env) " Rendering: " (:render complete-env))
      (respond-with complete-env))))
