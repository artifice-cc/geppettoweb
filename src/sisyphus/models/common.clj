(ns sisyphus.models.common
  (:require [com.ashafa.clutch :as clutch]))

(def db "http://localhost:5984/retrospect-new")

(def cachedir "/tmp")

(defn view
  ([name] (view name {} {}))
  ([name query] (view name query {}))
  ([name query post] (clutch/with-db db (clutch/get-view "app" name query post))))

(def get-doc-cache (ref {}))

(defn reset-doc-cache
  [id]
  (dosync (alter get-doc-cache dissoc [id])))

(defn this-memoize [cache f]
  (fn [& args]
    (when (<= 100 (count @cache))
      (println "Resetting cache.")
      (dosync (alter cache (constantly {}))))
    (if-let [e (find @cache args)]
      (val e)
      (let [ret (apply f args)]
        (dosync (alter cache assoc args ret))
        ret))))

(def get-doc
  (this-memoize
   get-doc-cache
   (fn
     ([id] (clutch/with-db db (clutch/get-document id)))
     ([id rev]
        (clutch/with-db db
          (let [revs (:_revisions (clutch/get-document id {:revs true}))]
            (assoc (clutch/get-document id {:rev rev} (constantly true)) :revs revs)))))))

(def get-many-docs
  (this-memoize
   get-doc-cache
   (fn [ids]
     (map :doc (:rows (clutch/with-db db (clutch/get-all-documents-meta
                                          {:include_docs true} {:keys ids})))))))

(defn create-doc
  ([data] (clutch/with-db db (clutch/create-document data)))
  ([data name] (clutch/with-db db (clutch/create-document data name))))

(defn delete-doc
  [doc]
  (reset-doc-cache (:_id doc))
  (try (clutch/with-db db (clutch/delete-document doc)) (catch Exception _)))

(defn get-attachment
  [id name] (clutch/with-db db (clutch/get-attachment id name)))

(defn to-clj
  [s]
  (try (read-string s)
       (catch Exception _)))
