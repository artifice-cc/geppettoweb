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
                       (filter (fn [c] ((set (map :runid (:runs c))) (:_id run))) all-claims)))))
  ([] (list-claims nil)))

(defn claim-select-options
  [run]
  (let [claims-map (list-claims)
        claims (filter (fn [c] (not ((set (map :runid (:runs c))) (:_id run))))
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

(defn update-claim
  [claim]
  (clutch/with-db local-couchdb
    (clutch/update-document (get-doc (:id claim))
                            (dissoc claim :_id :_rev :id :runs))))

(defn delete-claim
  [claim]
  (clutch/with-db local-couchdb
    (clutch/delete-document (get-doc (:id claim)))))

(defn add-claim-association
  [association]
  (let [claim (get-doc (:claim association))]
    (clutch/with-db local-couchdb
      (clutch/update-document claim #(conj % (dissoc association :claim)) [:runs]))))

(defn remove-claim-association
  [association]
  (let [claim (get-doc (:claim association))]
    (clutch/with-db local-couchdb
      (clutch/update-document
       claim (fn [runs] (filter (fn [r] (not= (:runid association)
                                              (:runid r)))
                                runs))
       [:runs]))))
