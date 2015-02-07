(defproject clojurewerkz/gizmo "1.0.0-alpha5-SNAPSHOT"
  :description "A Web development toolkit for Clojure"
  :url "https://github.com/clojurewerkz/gizmo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [enlive "1.1.5"]
                 [cheshire "5.4.0"]
                 [clojurewerkz/route-one "1.1.0"]
                 [com.taoensso/timbre "3.3.1"]

                 [ring/ring "1.3.2"]
                 [ring/ring-devel "1.3.2"]
                 [compojure "1.3.1"]

                 [bultitude "0.2.6"]]

  :source-paths ["src"]
  :profiles       {:dev {:resource-paths ["test/resources"]}})
