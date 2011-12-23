(ns sisyphus.models.annotations
  (:require [com.ashafa.clutch :as clutch])
  (:use sisyphus.models.common))

(defn add-annotation
  [id content]
  (let [doc (get-doc id)]
    (reset-doc-cache id)
    (clutch/with-db db
      (clutch/update-document doc #(conj % content) [:annotations]))))

(defn delete-annotation
  [id index]
  (clutch/with-db db
    (let [annotations (:annotations (clutch/get-document id))
          doc (get-doc id)]
      (reset-doc-cache id)
      (clutch/update-document
       doc {:annotations (concat (take index annotations)
                                 (drop (inc index) annotations))}))))
