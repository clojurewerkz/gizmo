(ns clojurewerkz.gizmo.widget-test
  (:require [net.cgrand.enlive-html :as html])
  (:use clojure.test
        clojurewerkz.gizmo.widget))

(defwidget testwidget-1
    :view (fn [a] a)
    :fetch (fn [s]
             (str (inc s))))

(deftest interplolate-widgets-test-simple
  (is (= "<div><h1>untouched 2</h1></div>"
         (render*
          (interpolate-widgets
           (html/html-snippet
            "<div><h1>untouched <widget rel=\"clojurewerkz.gizmo.widget-test/testwidget-1\"></widget></h1></div>")
           1)))))

(deftest interplolate-widgets-test-nested
  (defwidget testwidget-child
    :view (fn [a] a)
    :fetch (fn [s] (str (inc s))))

  (defwidget testwidget-parent
    :view (fn [_] (html/html-snippet "some <widget rel=\"clojurewerkz.gizmo.widget-test/testwidget-child\"></widget>"))
    :fetch (fn [_]))

  (is (= "<div><h1>untouched some 2</h1></div>"
         (render*
          (interpolate-widgets
           (html/html-snippet
            "<div><h1>untouched <widget rel=\"clojurewerkz.gizmo.widget-test/testwidget-parent\"></widget>")
           1)))))

(deftest interplolate-widgets-test-nested-parallel
  (defwidget testwidget-child
    :view (fn [a] a)
    :fetch (fn [s]
             (Thread/sleep 100)
             (str (inc (inc s)))))

  (defwidget testwidget-child-2
    :view (fn [a] a)
    :fetch (fn [s]
             (Thread/sleep 100)
             (str (inc s))))

  (defwidget testwidget-parent
    :view (fn [_] (html/html-snippet "some <div><widget rel=\"clojurewerkz.gizmo.widget-test/testwidget-child\"></widget></div> <div><widget rel=\"clojurewerkz.gizmo.widget-test/testwidget-child-2\"></widget></div>"))
    :fetch (fn [_]))

  (let [start (System/currentTimeMillis)]
    (render*
     (interpolate-widgets
      (html/html-snippet
       "<div><h1>untouched <widget rel=\"clojurewerkz.gizmo.widget-test/testwidget-parent\"></widget>")
      1))
    (is (< (- (System/currentTimeMillis) start) 200))))


(deftest inject-core-widgets-test
  (defwidget testwidget
    :view (fn [a] a)
    :fetch (fn [s] (str (inc s))))

  (testing "works with `traditional` widgets"
    (is (= "<div><h1>untouched 2</h1></div>"
           (render*
            (interpolate-widgets
             (inject-core-widgets
              (html/html-snippet
               "<div><h1>untouched <widget id=\"first-core-widget\"></widget></h1></div>")
              {:first-core-widget 'clojurewerkz.gizmo.widget-test/testwidget})
             1)))))
  (testing "works with fixed arguments"
    (is (= "<div><h1>untouched here we go</h1></div>"
           (render*
            (interpolate-widgets
             (inject-core-widgets
              (html/html-snippet
               "<div><h1>untouched <widget id=\"first-core-widget\"></widget></h1></div>")
              {:first-core-widget "here we go"})
             1)))))
  (testing "works with functions"
    (is (= "<div><h1>untouched 1</h1></div>"
           (render*
            (interpolate-widgets
             (inject-core-widgets
              (html/html-snippet
               "<div><h1>untouched <widget id=\"first-core-widget\"></widget></h1></div>")
              {:first-core-widget (comp identity str)})
             1))))))

(deftest layout-test
  (deflayout application "templates/layouts/application.html"
    [])
  (is (= application (:application (all-layouts)))))
