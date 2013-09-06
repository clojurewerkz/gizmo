(ns clojurewerkz.gizmo.enlive-test
  (:require [net.cgrand.enlive-html :as html])
  (:use clojure.test
        clojurewerkz.gizmo.widget
        clojurewerkz.gizmo.enlive))

(deftest defsnippet-test
  (defsnippet defsnippet-test-snippet-1 "templates/snippets/sniptest.html"
    [*content-main]
    [values]
    [*simple-list [*simple-list-item]] (html/clone-for [value values]
                                                       [html/any-node] (html/replace-vars {:value value})))

  (is (= (render* (defsnippet-test-snippet-1 ["a" "b" "c"]))
         "<div snippet=\"content-main\">
      <ul snippet=\"simple-list\">
        <li snippet=\"simple-list-item\">a</li><li snippet=\"simple-list-item\">b</li><li snippet=\"simple-list-item\">c</li>
      </ul>
    </div>")))
