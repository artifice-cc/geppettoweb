(ns sisyphus.models.parameters
  (:require [com.ashafa.clutch :as clutch])
  (:use sisyphus.models.common))

(defn new-parameters
  [params]
  (clutch/with-db local-couchdb
    (clutch/create-document
      (dissoc (assoc params :type "parameters") :id :action))))

(defn update-parameters
  [params]
  (clutch/with-db local-couchdb
    (clutch/update-document (clutch/get-document (:id params))
                            (dissoc params :id :_id :_rev :action))))

(defn list-parameters
  []
  (map :value
       (:rows (view "parameters" "list"
                    {:map (fn [doc]
                            (when (= "parameters" (:type doc))
                              [[[(:problem doc) (:name doc)] doc]]))}))))

(defn runs-with-parameters
  [params]
  (map :value
       (:rows (view "parameters" "runs-with-parameters"
                    {:map (fn [doc]
                            (when (= "run" (:type doc))
                              [[[(:paramsid doc) (:paramsrev doc)]
                                (assoc doc :control-count (count (:control doc))
                                       :comparison-count (count (:comparison doc))
                                       :comparative-count (count (:comparative doc)))]]))}
                    {:key [(:_id params) (:_rev params)]}))))
