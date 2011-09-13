(ns sisyphus.models.common
  (:require [com.ashafa.clutch :as clutch]))

(def local-couchdb "http://localhost:5984/retrospect")

(defn get-doc
  [id]
  (clutch/with-db local-couchdb
    (clutch/get-document id)))

