(ns clojurewerkz.gizmo.config-test
  (:use clojure.test
        clojurewerkz.gizmo.config))

(deftest load-from-test
  (is (= {:test :config} (load-from "test/resources/test_config.clj"))))

(deftest load-config!-test
  (load-config! "test/resources/test_config.clj")
  (is (= {:test :config} settings)))
