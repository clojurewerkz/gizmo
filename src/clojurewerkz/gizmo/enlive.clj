(ns clojurewerkz.gizmo.enlive
  (:require [net.cgrand.enlive-html :as html]))

(defn within
  [parent & children]
  (if (sequential? parent)
    (apply conj parent children)
    (apply conj [parent] children)))

(defn has-attr
 [attr]
 (html/pred #(not (nil? (-> % :attrs attr)))))

(defn snip
  [snippet-name]
  (html/pred #(= snippet-name (-> % :attrs :snippet))))

(defn format-selector
  [name]
  (format "*%s" name))

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

(defmacro defsnippet
 "Define a named snippet with enclosed selectors"
 [name source selector args & forms]
 (let [snippets (html/select (html/html-resource source) [(has-attr :snippet)])
       names    (apply concat (map #(vector
                                     (-> % :attrs :snippet format-selector symbol)
                                     (list 'snip (-> % :attrs :snippet))) snippets))]
   `(let* [~@names]
          (def ~name (html/snippet ~source ~selector ~args ~@forms)))))
