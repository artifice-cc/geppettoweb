(ns sisyphus.server
  (:require [noir.server :as server]))

(server/load-views "src/sisyphus/views/")

(defn -main [& m]
  (let [mode (keyword (or (first m) :dev))
        port (Integer. (get (System/getenv) "PORT" "3737"))]
    (server/start port {:mode mode
                        :ns 'sisyphus})
    ;; silly hack needed to use (sh)
    ;; see: http://stackoverflow.com/questions/7259072/
    ;;      clojure-java-shell-sh-throws-rejectedexecutionexception
    ;;      -when-run-in-a-new-thread
    @(promise)))

