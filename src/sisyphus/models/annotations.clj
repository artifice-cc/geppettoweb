(ns sisyphus.models.annotations
  (:require [com.ashafa.clutch :as clutch])
  (:use sisyphus.models.common))

(defn add-annotation
  [id content]
  (clutch/with-db db
    (clutch/update-document (get-doc id) #(conj % content) [:annotations])))

(defn delete-annotation
  [id index]
  (clutch/with-db db
    (let [annotations (:annotations (clutch/get-document id))]
      (clutch/update-document
       (get-doc id) {:annotations (concat (take index annotations)
                                          (drop (inc index) annotations))}))))
