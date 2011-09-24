(ns sisyphus.models.runs
  (:require [clojure.set :as set])
  (:require [com.ashafa.clutch :as clutch])
  (:use sisyphus.models.common))

(defn list-runs
  []
  (map :value
       (:rows (view "runs" "list"
                    {:map (fn [doc]
                            (when (= "run" (:type doc))
                              [[(:time doc)
                                (assoc doc :control-count (count (:control doc))
                                       :comparison-count (count (:comparison doc))
                                       :comparative-count (count (:comparative doc)))]]))}))))

(defn delete-run
  [id]
  (let [run (get-doc id)]
    (doseq [r (concat (:comparative run) (:control run) (:comparison run))]
      (clutch/with-db local-couchdb
        (clutch/delete-document (get-doc r))))
    (clutch/with-db local-couchdb
      (clutch/delete-document run))))

(defn problem-fields
  [problem]
  (let [rows (view "runs" "problem-fields"
                   {:map (fn [doc]
                           (when (= "comparative" (:type doc))
                             (for [field (keys doc)] [(:Problem doc) field])))
                    :reduce (fn [keys values rereduce]
                              (if rereduce (apply clojure.set/union values)
                                  (set values)))}
                   {:group true :group_level 1 :key problem})]
    (sort (set/difference (set (:value (first (:rows rows))))
                          #{"Problem" "Seed" "type" "runid" "_rev" "_id"}))))

(defn summarize-comparative-results
  [runid custom]
  (let [f (case (:func custom)
                "AVG" (fn [values] (double (/ (reduce + 0 values) (count values))))
                "SUM" (fn [values] (double (reduce + 0 values)))
                "MAX" (fn [values] (double (apply max values)))
                "MIN" (fn [values] (double (apply min values)))
                ;; default is SUM
                (fn [values] (double (apply + 0 values))))
        results (map :value
                     (:rows
                      (view "runs" "comparative"
                            {:map (fn [doc]
                                    (when (= "comparative" (:type doc))
                                      (for [field (keys doc)] [[(:runid doc) field] (get doc field)])))}
                            {:key [runid (:field custom)]})))]
    (if (and (not-empty results) (every? number? results))
      (f results))))

(defn add-annotation
  [id content]
  (clutch/with-db local-couchdb
    (-> (clutch/get-document id)
        (clutch/update-document #(conj % content) [:annotations]))))

(defn delete-annotation
  [id index]
  (clutch/with-db local-couchdb
    (let [annotations (:annotations (clutch/get-document id))]
      (-> (clutch/get-document id)
          (clutch/update-document {:annotations (concat (take index annotations)
                                                        (drop (inc index) annotations))})))))

(def dissoc-fields [:Problem :Step :runid :type :_rev :_id])

(defn get-fields
  [results]
  (if (= 0 (count results)) []
      (sort (apply set/intersection
                   (map (fn [r] (set (keys r)))
                        (map (fn [r] (apply dissoc r dissoc-fields)) results))))))

(defn get-results
  [id results-type]
  (map :value
       (:rows
        (eval `(clutch/with-db local-couchdb
                 (clutch/ad-hoc-view
                  (clutch/with-clj-view-server
                    {:map (fn [~'doc]
                            (when (and (= (name ~results-type) (:type ~'doc))
                                       (= ~id (:runid ~'doc)))
                              [[(:Seed ~'doc) ~'doc]]))})))))))

(defn query-comparative-results
  [fields limit]
  (clutch/with-db local-couchdb
    (let [results (map :doc (:rows
                             (view "runs" "query-comparative-results"
                                   {:map (fn [doc] (when (>= (:time doc) 0))
                                           (vec (map (fn [c] [nil {:_id c}]) (:comparative doc))))}
                                   {:include_docs true :limit limit})))]
      (map (fn [r] (select-keys r (conj fields :_id))) results))))

