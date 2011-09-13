(ns sisyphus.models.common
  (:require [com.ashafa.clutch :as clutch]))

(def local-couchdb
     {:host "localhost"
      :port 5984
      :username "sisyphus"
      :password "sisyphus"
      :name "retrospect"})

(defn get-doc
  [id]
  (clutch/with-db local-couchdb
    (clutch/get-document id)))

