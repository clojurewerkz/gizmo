(ns ^{:doc "Application configuration settings helper functions." }
  clojurewerkz.gizmo.config
  (:import [java.io File]))

;;
;; API
;;

(declare settings)

(defprotocol FileBasedConfiguration
  (expand    [path] "Expands filesystem path to be absolute")
  (load-from [path] "Reads and evaluates configuration from given resource."))

(extend-protocol FileBasedConfiguration
  File
  (expand    [f] (.getAbsolutePath f))
  (load-from [f] (read-string (with-open [rdr (clojure.java.io/reader f)]
                                (reduce str "" (line-seq rdr)))))

  String
  (expand    [s] (.getAbsolutePath (File. s)))
  (load-from [s] (load-from (File. s))))

(defn load-config!
  "Load config to `settings` variable. Usually called during server startup."
  [f]
  (def settings (load-from f)))
