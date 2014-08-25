(defproject clojurewerkz/gizmo "1.0.0-alpha4"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [enlive "1.1.5"]
                 [cheshire "5.3.1"]
                 [clojurewerkz/route-one "1.1.0"]
                 [com.taoensso/timbre "3.0.1"]

                 [ring/ring "1.2.1"]
                 [ring/ring-devel "1.2.1"]
                 [compojure "1.1.6"]

                 [bultitude "0.2.5"]]

  :source-paths ["src"]
  :profiles       {:dev {:resource-paths ["test/resources"]}})
