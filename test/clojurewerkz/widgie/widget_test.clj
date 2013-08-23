(ns clojurewerkz.widgie.widget-test
  (:require [net.cgrand.enlive-html :as html])
  (:use clojure.test
        clojurewerkz.widgie.widget))

(defwidget testwidget-1
    :view (fn [a] a)
    :fetch (fn [s]
             (str (inc s))))

(deftest interplolate-widgets-test-simple
  (is (= "<div><h1>untouched 2</h1></div>"
         (render*
          (interpolate-widgets
           (html/html-snippet
            "<div><h1>untouched <widget rel=\"clojurewerkz.widgie.widget-test/testwidget-1\"></widget></h1></div>")
           1)))))

(deftest interplolate-widgets-test-nested
  (defwidget testwidget-child
    :view (fn [a] a)
    :fetch (fn [s] (str (inc s))))

  (defwidget testwidget-parent
    :view (fn [_] (html/html-snippet "some <widget rel=\"clojurewerkz.widgie.widget-test/testwidget-child\"></widget>"))
    :fetch (fn [_]))

  (is (= "<div><h1>untouched some 2</h1></div>"
         (render*
          (interpolate-widgets
           (html/html-snippet
            "<div><h1>untouched <widget rel=\"clojurewerkz.widgie.widget-test/testwidget-parent\"></widget>")
           1)))))

(deftest interplolate-widgets-test-nested-parallel
  (defwidget testwidget-child
    :view (fn [a] a)
    :fetch (fn [s]
             (Thread/sleep 1000)
             (str (inc (inc s)))))

  (defwidget testwidget-child-2
    :view (fn [a] a)
    :fetch (fn [s]
             (Thread/sleep 1000)
             (str (inc s))))

  (defwidget testwidget-parent
    :view (fn [_] (html/html-snippet "some <div><widget rel=\"clojurewerkz.widgie.widget-test/testwidget-child\"></widget></div> <div><widget rel=\"clojurewerkz.widgie.widget-test/testwidget-child-2\"></widget></div>"))
    :fetch (fn [_]))

  (let [start (System/currentTimeMillis)]
    (render*
     (interpolate-widgets
      (html/html-snippet
       "<div><h1>untouched <widget rel=\"clojurewerkz.widgie.widget-test/testwidget-parent\"></widget>")
      1))
    (is (< (- (System/currentTimeMillis) start) 2000))))


(deftest inject-core-widgets-test
  (defwidget testwidget
    :view (fn [a] a)
    :fetch (fn [s] (str (inc s))))

  (is (= "<div><h1>untouched 2</h1></div>"
         (render*
          (interpolate-widgets
           (inject-core-widgets
            (html/html-snippet
             "<div><h1>untouched <widget id=\"first-core-widget\"></widget></h1></div>")
            {:first-core-widget 'clojurewerkz.widgie.widget-test/testwidget})
           1)))))
