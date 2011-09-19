(ns sisyphus.models.claims
  (:require [com.ashafa.clutch :as clutch])
  (:use sisyphus.models.common))

(defn list-claims
  ([run]
     (let [all-claims
           (map :value
                (:rows
                 (clutch/with-db local-couchdb
                   (clutch/ad-hoc-view
                    (clutch/with-clj-view-server
                      {:map (fn [doc]
                              (when (= "claim" (:type doc))
                                [[(:created doc) doc]]))})))))]
       (reduce (fn [m c] (update-in m [(if (= "Unverified" (:verification c)) :unverified :verified)]
                                    conj c))
               {:unverified [] :verified []}
               (if-not run all-claims
                       (filter (fn [c] ((set (:runs c)) (:_id run))) all-claims)))))
  ([] (list-claims nil)))

(defn claim-select-options
  [run]
  (let [claims-map (list-claims)
        claims (filter (fn [c] (not ((set (:runs c)) (:_id run))))
                       (concat (:unverified claims-map) (:verified claims-map)))]
    (map (fn [c] [(format "%s (%s)" (:title c) (:verification c)) (:_id c)]) claims)))

(defn new-claim
  [claim]
  (clutch/with-db local-couchdb
    (clutch/create-document
     (assoc (dissoc claim :id)
       :type "claim"
       :runs []
       :custom-map {}
       :created (System/currentTimeMillis)))))

(defn add-claim-association
  [runid claimid]
  (let [claim (get-doc claimid)]
    (clutch/with-db local-couchdb
      (clutch/update-document claim #(conj % runid) [:runs]))))
