(ns clojurewerkz.gizmo.widget
  (:require [net.cgrand.enlive-html :as html]
            [clojure.core.reducers :as r]
            [clojure.string :as str]
            [net.cgrand.xml :as xml]
            [bultitude.core :as bultitude]))

(defn select-values [map ks]
  (reduce #(conj %1 (map %2)) [] ks))

;;
;; Implementation
;;

(defn default-fetch
  [_]
  {})

(defn default-view
  [_]
  "")

(defn render*
  [t]
  (apply str (html/emit* t)))

(defn- resolve-widget
  [s]
  (assert (get-in s [:attrs :rel]) "Can't resolve widget name. If it's an top-level widget, please add {:widgets ...} clause to your responder, otherwise add `rel` attribute to widget for proper resolution.")
  (if-let [widget (resolve (symbol (get-in s [:attrs :rel])))]
    widget
    (throw (Exception. (str "Can't resolve widget " s ". " (get-in s [:attrs :rel]) " is not found")))))

(defn in?
  "true if seq contains elm"
  [seq elm]
  (not (nil? (some #(= elm %) seq))))

;;
;; API
;;

(defmacro defwidget
  [widget-name &{:keys [fetch view] :or {fetch         default-fetch
                                         view          default-view}}]
  (let [opts {:name (keyword widget-name) :fetch fetch :view view}]
    `(def ~(vary-meta widget-name assoc :widget true :opts opts)
       (fn [env#] (~view (~fetch env#))))))

(defmacro layout*
  ""
  [source args & forms]
  `(html/snippet* (html/html-resource ~source) ~args ~@forms))

(defmacro deflayout
  [layout-name source args & forms]
  `(def ~(vary-meta layout-name assoc :layout true :source source)
     (layout* ~source ~args ~@forms)))

(defn inject-core-widgets
  [html-source widgets]
  (html/flatmap
   (html/transformation
    [:widget] (fn [node]
                (let [^symbol widget-id (get-in node [:attrs :id])]
                  (if-let [rel (get widgets (keyword widget-id))]
                    (assoc-in node [:attrs :rel] rel)
                    node))))
   html-source))

;; TODO Add widget cache for widgets that were already rendered in different context so that they wouldn't be re-rendered
(defn interpolate-widgets
  [html-source env]
  (let [step-widgets (into {}
                           (filter identity
                                   (pmap (fn [w]
                                           (when-let [widget-fn (resolve-widget w)]
                                             [widget-fn (widget-fn env)]))
                                         (html/select html-source [:widget]))))]
    (html/flatmap
     (html/transformation
      [:widget] (fn [widget]
                  (let [widget-fn (resolve-widget widget)
                        view (get step-widgets widget-fn)]
                    (if (seq? view)
                      (interpolate-widgets view env)
                      view))))
     html-source)))

(defn all-layouts
  []
  (->> (all-ns)
       (map #(vals (ns-interns %)))
       flatten
       (filter #(:layout (meta %)))
       (map #(vector (keyword (:name (meta %))) (var-get %)))
       (into {})))
