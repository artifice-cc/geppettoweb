(ns sisyphus.server
  (:import (java.io File))
  (:use [clojure.contrib.command-line :only [with-command-line]])
  (:use [granary.misc])
  (:use [korma.db :only [create-db mysql]])
  (:use [sisyphus.models.common :only [sisyphus-db cachedir]])
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
  (with-command-line args
    "sisyphus"
    [[port "Port" "3737"]
     [granary-dbhost "Granary MySQL database host" "localhost"]
     [granary-dbname "Granary MySQL database name" "granarydb"]
     [granary-dbuser "Granary MySQL database user" "user"]
     [granary-dbpassword "Granary MySQL database password" "password"]
     [sisyphus-dbhost "Sisyphus MySQL database host" "localhost"]
     [sisyphus-dbname "Sisyphus MySQL database name" "sisyphusdb"]
     [sisyphus-dbuser "Sisyphus MySQL database user" "user"]
     [sisyphus-dbpassword "Sisyphus MySQL database password" "password"]
     [cache "Cache directory" "cache"]]
    (.mkdirs (File. cache))
    (set-granary-db granary-dbhost granary-dbname granary-dbuser granary-dbpassword)
    (dosync
     (alter sisyphus-db
            (constantly
             (create-db (mysql {:host sisyphus-dbhost
                                :db sisyphus-dbname
                                :user sisyphus-dbuser
                                :password sisyphus-dbpassword}))))
     (alter cachedir (constantly cache)))
    (server/start (Integer/parseInt port)
                  {:mode :dev
                   :ns 'sisyphus})
    ;; silly hack needed to use (sh)
    ;; see: http://stackoverflow.com/questions/7259072/
    ;;      clojure-java-shell-sh-throws-rejectedexecutionexception
    ;;      -when-run-in-a-new-thread
    @(promise)))

(def handler (server/gen-handler {:ns 'sisyphus}))

