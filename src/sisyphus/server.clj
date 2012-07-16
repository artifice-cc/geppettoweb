(ns sisyphus.server
  (:require [noir.server :as server])
  (:require [sisyphus.views.claims]
            [sisyphus.views.configure]
            [sisyphus.views.run]
            [sisyphus.views.run-tables]
            [sisyphus.views.simulation]
            [sisyphus.views.graphs]
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

(def handler (server/gen-handler {:ns 'sisyphus}))

