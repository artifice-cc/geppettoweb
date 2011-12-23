(ns sisyphus.models.claims
  (:require [com.ashafa.clutch :as clutch])
  (:use sisyphus.models.common))

(defn list-claims
  ([run]
     (let [all-claims (map :value (:rows (view "claims-list")))]
       (reduce (fn [m c] (update-in m [(if (= "Unverified" (:verification c))
                                         :unverified :verified)]
                                    conj c))
               {:unverified [] :verified []}
               (if-not run all-claims
                       (filter (fn [c] ((set (map :runid (:runs c))) (:_id run)))
                               all-claims)))))
  ([] (list-claims nil)))

(defn claim-select-options
  [run]
  (let [claims-map (list-claims)
        claims (filter (fn [c] (not ((set (map :runid (:runs c))) (:_id run))))
                       (concat (:unverified claims-map) (:verified claims-map)))]
    (map (fn [c] [(format "%s (%s)" (:title c) (:verification c)) (:_id c)]) claims)))

(defn new-claim
  [claim]
  (create-doc
   (assoc (dissoc claim :id)
     :type "claim"
     :runs []
     :custom-map {}
     :created (System/currentTimeMillis))))

(defn update-claim
  [claim]
  (let [doc (get-doc (:id claim))]
    (reset-doc-cache (:id claim))
    (clutch/with-db db
      (clutch/update-document doc (dissoc claim :_id :_rev :id :runs)))))

(defn delete-claim
  [id]
  (delete-doc (get-doc id)))

(defn add-claim-association
  [association]
  (let [claim (get-doc (:claim association))]
    (reset-doc-cache (:claim association))
    (clutch/with-db db
      (clutch/update-document
       claim #(conj % (dissoc association :claim)) [:runs]))))

(defn get-claim-association
  [claim run]
  (first (filter (fn [r] (= (:runid r) (:_id run))) (:runs claim))))

(defn remove-claim-association
  [association]
  (let [claim (get-doc (:claim association))]
    (reset-doc-cache (:claim association))
    (clutch/with-db db
      (clutch/update-document
       claim (fn [runs] (filter (fn [r] (not= (:runid association) (:runid r))) runs))
       [:runs]))))
