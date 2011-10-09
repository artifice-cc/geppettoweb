(ns sisyphus.models.configuration
  (:require [com.ashafa.clutch :as clutch])
  (:use sisyphus.models.common))

(defn get-configuration
  []
  (if-let [config (get-doc "configuration")] config
          (create-doc {} "configuration")))

(defn update-configuration
  [config]
  (clutch/with-db db
    (clutch/update-document (get-configuration) config)))

(defn build-remote
  [config]
  {:host (:remote-host config)
   :port (:remote-port config)
   :username (:remote-username config)
   :password (:remote-password config)
   :name (:remote-name config)})

(defn do-replication
  []
  (let [config (get-configuration)
        remote (build-remote config)]
    (clutch/with-db db
      [(clutch/replicate-database (clutch/get-database db) remote)
       (clutch/replicate-database remote (clutch/get-database db))])))
