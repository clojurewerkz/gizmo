(defproject clojurewerkz/gizmo "1.0.0-alpha5-SNAPSHOT"
  :description "A Web development toolkit for Clojure"
  :url "https://github.com/clojurewerkz/gizmo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure        "1.7.0"]
                 [com.stuartsierra/component "0.2.3"]
                 [cheshire                   "5.4.0"]
                 [clojurewerkz/route-one     "1.1.0"]
                 [ring/ring                  "1.4.0"]
                 [ring/ring-devel            "1.4.0"]
                 [compojure                  "1.4.0"]
                 [org.clojure/tools.logging  "0.3.1"]
                 [bultitude                  "0.2.8"]]

  :source-paths ["src"]
  :profiles       {:dev {:resource-paths ["test/resources"]}})
