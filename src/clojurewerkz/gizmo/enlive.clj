(ns clojurewerkz.gizmo.enlive
  "Enlive helper functions and extensions"
  (:require [net.cgrand.enlive-html :as html]))

(defn- format-selector
  [name]
  (format "*%s" name))

;;
;; API
;;

(defn within
  "Selector helper to that specifies that certain selector is located within the other one,
   for example:

     (within :ul [:li html/first-of-type])

   Queries for first li element located within ul element."
  [parent & children]
  (if (sequential? parent)
    (apply conj parent children)
    (apply conj [parent] children)))

(defn has-attr
  "Predicare function that fetches only the elements that have certain attribute, for example:

      (has-attr :id)

   Will only return elements that have an `id` attribute"
  [attr]
  (html/pred #(not (nil? (-> % :attrs attr)))))

(defn snip
  "Predicate function to retrieve all snippets from the view"
  [snippet-name]
  (html/pred #(= snippet-name (-> % :attrs :snippet))))

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
  "Define a named snippet with enclosed selectors. Will query `source` for the elements
   that have `snippet` attribute and turn values of `snippet` attribute into selectors,
   for example, if you have `<div snippet='main_content'></div>`, you'll have main_content*
   selector available within the scope of snippet definition."
 [name source selector args & forms]
 (let [snippets (html/select (html/html-resource source) [(has-attr :snippet)])
       names    (apply concat (map #(vector
                                     (-> % :attrs :snippet format-selector symbol)
                                     (list 'clojurewerkz.gizmo.enlive/snip (-> % :attrs :snippet))) snippets))]
   `(let* [~@names]
          (def ~name (html/snippet ~source ~selector ~args ~@forms)))))
