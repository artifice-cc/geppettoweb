(ns geppettoweb.server
  (:import (java.io File))
  (:use [clojure.tools.cli :only [cli]])
  (:use [clojure.contrib.command-line :only [with-command-line]])
  (:use [geppettoweb.config])
  (:require [noir.server :as server])
  (:require [geppettoweb.views.run]
            [geppettoweb.views.tables]
            [geppettoweb.views.graphs]
            [geppettoweb.views.analyses]
            [geppettoweb.views.overview]
            [geppettoweb.views.parameters]
            [geppettoweb.views.results]))

(server/load-views-ns 'geppettoweb.views)

(defn cache-control
  [handler]
  (fn [request]
    (let [resp (handler request)]
      (if (re-matches #".*update.*" (:uri request))
        (assoc-in resp [:headers "Cache-Control"] "public, max-age=0")
        (assoc-in resp [:headers "Cache-Control"] "public, max-age=5")))))

(server/add-middleware cache-control)

(defn -main [& args]
  (let [[options _ banner]
        (cli args
             ["--config" "Config file (default: config.properties)" :default "config.properties"])]
    (load-config (:config options))
    (server/start (Integer/parseInt @port)
                  {:mode :dev
                   :ns 'geppettoweb}))
  ;; silly hack needed to use (sh)
  ;; see: http://stackoverflow.com/questions/7259072/
  ;;      clojure-java-shell-sh-throws-rejectedexecutionexception
  ;;      -when-run-in-a-new-thread
  @(promise))

(def handler (server/gen-handler {:ns 'geppettoweb}))

