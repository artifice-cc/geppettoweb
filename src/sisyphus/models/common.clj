(ns sisyphus.models.common
  (:require [com.ashafa.clutch :as clutch]))

(def local-couchdb
     {:host "localhost"
      :port 5984
      :username "sisyphus"
      :password "4gGL3n59zupV0f"
      :name "retrospect"})

(defmacro view
  [name1 name2 body & opts]
  `(clutch/with-db local-couchdb
     (if-let [results# (clutch/get-view ~name1 ~name2 ~@opts)]
       results#
       (do
         (clutch/save-view ~name1 ~name2 (clutch/with-clj-view-server ~body))
         (clutch/get-view ~name1 ~name2 ~@opts)))))

(defn get-doc
  ([id]
     (clutch/with-db local-couchdb
       (clutch/get-document id)))
  ([id rev]
     (clutch/with-db local-couchdb
       (let [revs (:_revisions (clutch/get-document id {:revs true}))]
         (assoc
             (clutch/get-document id {:rev rev} (constantly true))
           :revs revs)))))
