(ns sisyphus.models.common
  (:require [com.ashafa.clutch :as clutch]))

(def db "http://localhost:5984/retrospect")

(def cachedir "/tmp")

(defn view
  ([name] (view name {} {}))
  ([name query] (view name query {}))
  ([name query post] (clutch/with-db db (clutch/get-view "app" name query post))))

(defn get-doc
  ([id] (clutch/with-db db (clutch/get-document id)))
  ([id rev]
     (clutch/with-db db
       (let [revs (:_revisions (clutch/get-document id {:revs true}))]
         (assoc (clutch/get-document id {:rev rev} (constantly true)) :revs revs)))))

(defn create-doc
  ([data] (clutch/with-db db (clutch/create-document data)))
  ([data name] (clutch/with-db db (clutch/create-document data name))))

(defn delete-doc
  [doc] (clutch/with-db db (clutch/delete-document doc)))

(defn get-attachment
  [id name] (clutch/with-db db (clutch/get-attachment id name)))

(defn to-clj
  [s]
  (try (read-string s)
       (catch Exception _)))
