(ns clojurewerkz.gizmo.responder-test
  (:use clojure.test
        clojurewerkz.gizmo.responder
        clojurewerkz.gizmo.widget
        clojurewerkz.gizmo.request))

(deftest respond-with-html
  (deflayout respond-with-html-layout "templates/layouts/application.html" [])
  (defwidget respond-with-html-header-widget :view (fn [_] "header"))
  (defwidget respond-with-html-content-widget :view (fn [_] "content"))

  (let [res (respond-with {:render  :html
                           :widgets {:content 'clojurewerkz.gizmo.responder-test/respond-with-html-content-widget
                                     :header 'clojurewerkz.gizmo.responder-test/respond-with-html-header-widget}})]
    (is (= "173" (get-in res [:headers "Content-Length"])))
    (is (= "text/html; charset=utf-8" (get-in res [:headers "Content-Type"])))
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
    (is (= "19" (get-in res [:headers "Content-Length"])))
    (is (= "application/json; charset=utf-8" (get-in res [:headers "Content-Type"])))
    (is (= "{\"response\":\"hash\"}" (:body res)))
    (is (= 200 (get-in res [:status])))))


(deftest respond-with-custom-status
  (let [res (respond-with {:render :json
                           :status 404
                           :response-hash {:response :hash}})]
    (is (= 404 (:status res)))))

(deftest wrap-responder-test
  (let [env     (atom nil)
        handler (wrap-responder
                 (fn [e]
                   (reset! env e)
                   {:response-hash {:response :hash}
                    :status 201
                    :render :json}))
        res     (handler {:env :env})]
    (testing "Handler receives env from middleware"
      (is (= {:env :env} @env)))
    (testing "Response depends on outcome of handler"
      (is (= 201 (:status res)))
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
    (is (= "208" (get-in res [:headers "Content-Length"])))
    (is (= "text/html; charset=utf-8" (get-in res [:headers "Content-Type"])))
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

(deftest custom-responder
  (defmethod respond-with :whatev
    [env]
    {:status 200
     :body "whatev"})

  (is (= {:status 200 :body "whatev"} (respond-with {:render :whatev}))))
