(ns clojurewerkz.gizmo.responder-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojurewerkz.gizmo.responder :refer [respond-with wrap-responder]]
            [clojurewerkz.gizmo.widget :refer [defwidget deflayout]]
            [clojurewerkz.gizmo.request :refer [request]]))

(deftest respond-with-html
  (deflayout respond-with-html-layout "templates/layouts/application.html" [])
  (defwidget respond-with-html-header-widget :view (fn [_] "header"))
  (defwidget respond-with-html-content-widget :view (fn [_] "content"))

  (let [res (respond-with {:render  :html
                           :widgets {:content 'clojurewerkz.gizmo.responder-test/respond-with-html-content-widget
                                     :header 'clojurewerkz.gizmo.responder-test/respond-with-html-header-widget}})]
    (is (= "173" (get-in res [:headers "content-length"])))
    (is (= "text/html; charset=utf-8" (get-in res [:headers "content-type"])))
    (is (= "<!DOCTYPE html>
<html lang=\"en\">
  <head>
    <title>App Layout</title>
  </head>
  <body>
    header
    <div id=\"content-main\">
      content
    </div>
  </body>

</html>" (:body res)))
    (is (= 200 (get-in res [:status])))))

(deftest respond-with-json
  (let [res (respond-with {:render        :json
                           :response-hash {:response :hash}})]
    (is (= "19" (get-in res [:headers "content-length"])))
    (is (= "application/json; charset=utf-8" (get-in res [:headers "content-type"])))
    (is (= "{\"response\":\"hash\"}" (:body res)))
    (is (= 200 (get-in res [:status])))))


(deftest respond-with-custom-status
  (let [res (respond-with {:render :json
                           :status 404
                           :response-hash {:response :hash}})]
    (is (= 404 (:status res)))))

(deftest respond-with-cookies
  (let [cookies {"theCookie" {}}
        res (respond-with {:render :json
                           :status 404
                           :response-hash {:response :hash}
                           :cookies cookies})]
    (is (= cookies (:cookies res)))))

(deftest wrap-responder-test
  (let [env     (atom nil)
        handler (wrap-responder
                 (fn [e]
                   (reset! env e)
                   {:response-hash {:response :hash}
                    :status 201
                    :render :json}))
        res     (handler {:env :env
                          :cookies {:foo "bar"}})]
    (testing "Handler receives env from middleware"
      (is (= {:env :env :cookies {:foo "bar"}} @env)))
    (testing "Response depends on outcome of handler"
      (is (= 201 (:status res)))
      (is (= {} (:cookies res)))
      (is (= "{\"response\":\"hash\"}" (:body res))))))

(deftest respond-with-html-with-request
  (deflayout respond-with-html-layout "templates/layouts/application.html" [])
  (defwidget respond-with-html-header-widget :view (fn [_] (str "header" (:header-info (request)))))
  (defwidget respond-with-html-content-widget :view (fn [_] (str "content" (:content-info (request)))))

  (let [res (respond-with {:render      :html
                           :header-info " with header info"
                           :content-info " with content info"
                           :widgets {:content 'clojurewerkz.gizmo.responder-test/respond-with-html-content-widget
                                     :header 'clojurewerkz.gizmo.responder-test/respond-with-html-header-widget}})]
    (is (= "208" (get-in res [:headers "content-length"])))
    (is (= "text/html; charset=utf-8" (get-in res [:headers "content-type"])))
    (is (= "<!DOCTYPE html>
<html lang=\"en\">
  <head>
    <title>App Layout</title>
  </head>
  <body>
    header with header info
    <div id=\"content-main\">
      content with content info
    </div>
  </body>

</html>" (:body res)))
    (is (= 200 (get-in res [:status])))))

(deftest respond-with-redirect
  (let [location "http://example.com"
        accept "application/json"
        res (respond-with {:render :redirect
                           :location location
                           :headers {"Accept" accept}})]
    (is (= location (get-in res [:headers "Location"])))
    (is (= accept (get-in res [:headers "Accept"])))
    (is (= 302 (:status res)))))

(deftest custom-responder
  (defmethod respond-with :whatev
    [env]
    {:status 200
     :body "whatev"})

  (is (= {:status 200 :body "whatev"} (respond-with {:render :whatev}))))
