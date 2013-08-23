(ns clojurewerkz.widgie.widget
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
  (resolve (symbol (get-in s [:attrs :rel]))))

(defn in?
  "true if seq contains elm"
  [seq elm]
  (not (nil? (some #(= elm %) seq))))

;;
;; API
;;

(defmacro defselector
  ^{:doc "Defining a selector and helper functions for it, for example:

    (utils/defselector product-list-item [[:article.article html/first-of-type]])

    Generates a def that you can use whenever you refer that selector:
     (clojure.core/defonce *product-list-item [[:article.article html/first-of-type]])

    And a selection helper, you should pass html-source into it and it will return a desired element, that you can use in your tests

      (defn select-product-list-item
       [source]
       (html/select source [[:article.article html/first-of-type]]))"}
  [name value]
  `(do (def ~(symbol (format "*%s" name)) ~value)
       (defn ~(symbol (format "select-%s" name)) [source#] (html/select source# ~value))))

(defmacro defwidget
  [widget-name &{:keys [fetch view] :or {fetch         default-fetch
                                         view          default-view}}]
  (let [opts {:name (keyword widget-name) :fetch fetch :view view}]
    `(def ~(vary-meta widget-name assoc :widget true :opts opts)
       (fn [env#] (~view (~fetch env#))))))

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

(defn require-widgets
  [prefix]
  (doseq [ns (bultitude/namespaces-on-classpath :prefix prefix)]
    (require ns)))
