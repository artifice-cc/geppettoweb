(ns sisyphus.models.runs
  (:require [clojure.set :as set])
  (:require [com.ashafa.clutch :as clutch]))

(def local-couchdb "http://localhost:5984/retrospect")

(defn list-runs
  []
  (:rows
   (clutch/with-db local-couchdb
     (clutch/ad-hoc-view
      (clutch/with-clj-view-server
        {:map (fn [doc]
                (when (:problem doc)
                  [[(:time doc)
                    {:problem (:problem doc)
                     :control-strategy (:control-strategy doc)
                     :comparison-strategy (:comparison-strategy doc)
                     :control-count (count (:control doc))
                     :comparison-count (count (:comparison doc))
                     :comparative-count (count (:comparative doc))
                     :commit (:commit doc)
                     :hostname (:hostname doc)
                     :repetitions (:repetitions doc)
                     :time (:time doc)}]]))})))))

(defn problem-fields
  [problem]
  (let [rows (clutch/with-db local-couchdb
               (clutch/ad-hoc-view
                (clutch/with-clj-view-server
                  {:map (fn [doc]
                          (when (= "comparative" (:type doc))
                            (for [field (keys doc)] [(:Problem doc) field])))
                   :reduce (fn [keys values rereduce]
                             (if rereduce (apply clojure.set/union values)
                                 (set values)))})
                {:group true :group_level 1 :key problem}))]
    (sort (set/difference (set (:value (first (:rows rows))))
                          #{"Control" "Comparison" "Problem" "Seed" "type" "runid" "_rev" "_id"}))))

(defmacro summarize-comparative-results
  [runid custom]
  `(let [~'field (:field ~custom)
         ~'f (case (:func ~custom)
                   "AVG" '(fn [~'values] (double (/ (reduce + 0 ~'values) (count ~'values))))
                   "SUM" '(fn [~'values] (reduce + 0 ~'values))
                   "MAX" '(fn [~'values] (apply max ~'values))
                   "MIN" '(fn [~'values] (apply min ~'values))
                   ;; default is SUM
                   '(fn [~'values] (apply + 0 ~'values)))]
     `(clutch/with-db local-couchdb
        (clutch/ad-hoc-view
         (clutch/with-clj-view-server
           {:map (fn [~'~'doc]
                   (when (= "comparative" (:type ~'~'doc))
                     (for [~'~'field (keys ~'~'doc) :when (number? (get ~'~'doc ~'~'field))]
                       [[(:runid ~'~'doc) ~'~'field] (get ~'~'doc ~'~'field)])))
            :reduce (fn [~'~'_ ~'~'values ~'~'_] (~~'f ~'~'values))})
         {:key [~~runid ~~'field]}))))

(defn get-doc
  [id]
  (clutch/with-db local-couchdb
    (clutch/get-document id)))

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

(def dissoc-fields [:Problem :Comparison :Control :Step :runid :type :_rev :_id])

(defn get-fields
  [results]
  (sort (apply set/intersection
               (map (fn [r] (set (keys r)))
                    (map (fn [r] (apply dissoc r dissoc-fields)) results)))))

(defn get-results
  [id results-type]
  (let [results (eval `(clutch/with-db local-couchdb
                         (clutch/ad-hoc-view
                          (clutch/with-clj-view-server
                            {:map (fn [~'doc]
                                    (when (= (name ~results-type) (:type ~'doc))
                                      [[(:runid ~'doc) ~'doc]]))})
                          {:key ~id})))]
    (sort-by :Seed (map :value (:rows results)))))

(defn query-comparative-results
  [fields limit]
  (clutch/with-db local-couchdb
    (let [results (map :doc (:rows (clutch/ad-hoc-view
                                    (clutch/with-clj-view-server
                                      {:map (fn [doc] (when (>= (:time doc) 0))
                                              (vec (map (fn [c] [nil {:_id c}]) (:comparative doc))))})
                                    {:include_docs true :limit limit})))]
      (map (fn [r] (select-keys r (conj fields :_id))) results))))

