(ns sisyphus.server
  (:import (java.io File))
  (:use [clojure.contrib.command-line :only [with-command-line]])
  (:use [sisyphus.config])
  (:require [noir.server :as server])
  (:require [sisyphus.views.run]
            [sisyphus.views.tables]
            [sisyphus.views.graphs]
            [sisyphus.views.analyses]
            [sisyphus.views.overview]
            [sisyphus.views.parameters]
            [sisyphus.views.results]))

(server/load-views-ns 'sisyphus.views)

(defn cache-control
  [handler]
  (fn [request]
    (let [resp (handler request)]
      (if (re-matches #".*update.*" (:uri request))
        (assoc-in resp [:headers "Cache-Control"] "public, max-age=0")
        (assoc-in resp [:headers "Cache-Control"] "public, max-age=5")))))

(server/add-middleware cache-control)

(defn -main [& args]
  (load-config)
  (.mkdirs (File. @cachedir))
  (server/start (Integer/parseInt @port)
                {:mode :dev
                 :ns 'sisyphus})
  ;; silly hack needed to use (sh)
  ;; see: http://stackoverflow.com/questions/7259072/
  ;;      clojure-java-shell-sh-throws-rejectedexecutionexception
  ;;      -when-run-in-a-new-thread
  @(promise))

(def handler (server/gen-handler {:ns 'sisyphus}))

