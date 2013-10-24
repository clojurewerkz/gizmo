(defproject clojurewerkz/gizmo "1.0.0-alpha3-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [enlive "1.1.4"]
                 [cheshire "5.0.2"]
                 [clojurewerkz/route-one "1.0.0-rc3"]

                 [ring/ring "1.2.0"]
                 [ring/ring-devel "1.2.0"]
                 [compojure "1.1.5"]

                 [bultitude "0.2.2"]

                 [com.taoensso/timbre "2.6.3"]]

  :source-paths ["src"]
  :profiles       {:dev {:resource-paths ["test/resources"]}})
