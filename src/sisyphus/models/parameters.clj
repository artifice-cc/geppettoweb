(ns sisyphus.models.parameters
  (:require [com.ashafa.clutch :as clutch])
  (:use sisyphus.models.common))

(defn new-parameters
  [params]
  (clutch/with-db local-couchdb
    (clutch/create-document (assoc params :type "parameters"))))

(defn update-parameters
  [params]
  (clutch/with-db local-couchdb
    (clutch/update-document (clutch/get-document (:id params))
                            (dissoc params :id :_id :_rev))))

(defn list-parameters
  []
  (map :value
       (:rows (clutch/with-db local-couchdb
                (clutch/ad-hoc-view
                 (clutch/with-clj-view-server
                   {:map (fn [doc]
                           (when (= "parameters" (:type doc))
                             [[[(:problem doc) (:name doc)] doc]]))}))))))

(defn runs-with-parameters
  [params]
  (map :value
       (:rows (clutch/with-db local-couchdb
                (clutch/ad-hoc-view
                 (clutch/with-clj-view-server
                   {:map (fn [doc]
                           (when (= "run" (:type doc))
                             [[[(:paramsid doc) (:paramsrev doc)]
                               (assoc doc :control-count (count (:control doc))
                                      :comparison-count (count (:comparison doc))
                                      :comparative-count (count (:comparative doc)))]]))})
                 {:key [(:_id params) (:_rev params)]})))))
