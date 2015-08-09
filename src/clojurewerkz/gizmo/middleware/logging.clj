(ns clojurewerkz.gizmo.middleware.logging
  (:require [clojure.string :as s]
            [clojure.tools.logging :as log]))

(defn wrap-logger
  [handler]
  (fn [env]
    (let [start (System/currentTimeMillis)
          res   (handler env)]

      (log/info (format "Processing '%s' (for '%s' at %s) [%s]"
                    (:uri env)
                    (get-in env [:headers "host"])
                    start
                    (-> env :request-method name s/upper-case)))

      (log/info (format
             "Completed '%s' in %sms, status: %s\n"
             (:uri env)
             (- (System/currentTimeMillis) start)
             (:status res)))
      res)))
