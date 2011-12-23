(ns sisyphus.models.parameters
  (:require [com.ashafa.clutch :as clutch])
  (:use sisyphus.models.common))

(defn new-parameters
  [params]
  (create-doc
   (dissoc (assoc params :type "parameters") :id :action)))

(defn update-parameters
  [params]
  (let [doc (get-doc (:id params))]
    (reset-doc-cache (:id params))
    (clutch/with-db db
      (clutch/update-document doc (dissoc params :id :_id :_rev :action)))))

(defn list-parameters
  []
  (reduce (fn [m ps] (update-in m [(keyword (:paramstype ps))] conj ps))
          {:comparative [] :non-comparative []}
          (map :value (:rows (view "parameters-list")))))

(defn runs-with-parameters
  [params]
  (map :value (:rows (view "parameters-runs" {:key [(:_id params) (:_rev params)]}))))

(defn delete-parameters
  [id]
  (delete-doc (get-doc id)))
