(ns sisyphus.models.runs
  (:require [clojure.set :as set])
  (:require [clojure.java.io :as io])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.claims :only [list-claims remove-claim-association]])
  (:use sisyphus.models.common))

(defn list-runs
  []
  (map :value (:rows (view "runs-list"))))

(defn delete-run
  [id]
  (let [run (get-doc id)
        claims (apply concat (vals (list-claims run)))]
    (doseq [c claims]
      (remove-claim-association {:claim (:_id c) :runid id}))
    (doseq [r (concat (:comparative run) (:control run) (:comparison run))]
      (delete-doc (get-doc r)))
    (delete-doc run)))

(defn problem-fields
  [problem]
  (let [fields (map :key (:rows (view "problem-fields" {:group true :group_level 2})))]
    (sort (map second (filter #(= problem (first %)) fields)))))

(defn summarize-comparative-results
  [runid custom]
  (let [f (case (:func custom)
                "AVG" (fn [values] (double (/ (reduce + 0 values) (count values))))
                "SUM" (fn [values] (double (reduce + 0 values)))
                "MAX" (fn [values] (double (apply max values)))
                "MIN" (fn [values] (double (apply min values)))
                ;; default is SUM
                (fn [values] (double (apply + 0 values))))
        results (map :value (:rows (view "comparative-results" {:key [runid (:field custom)]})))]
    (if (and (not-empty results) (every? number? results))
      (f results))))

(defn add-annotation
  [id content]
  (-> (get-doc id)
      (clutch/with-db db (clutch/update-document #(conj % content) [:annotations]))))

(defn delete-annotation
  [id index]
  (let [annotations (:annotations (clutch/get-document id))]
    (-> (get-doc id)
        (clutch/with-db db
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
  (let [results-ids (get (get-doc id) results-type)]
    (sort-by :Seed (for [i results-ids] (get-doc i)))))

(def cachedir "/tmp")

(defn csv-filenames
  [run]
  (zipmap [:control :comparison :comparative]
          (map #(format "%s/%s-%s.csv" cachedir (:_id run) (name %))
               [:control :comparison :comparative])))

(defn format-csv-row
  [row]
  ;; add quotes around string data
  (apply str (concat (interpose "," (map #(if (= String (type %)) (format "\"%s\"" %) %) row))
                     [\newline])))

(defn results-to-csv
  [run csv-fnames]
  (doseq [results-type (keys csv-fnames)]
    (let [outfile (io/file (get csv-fnames results-type))]
      (when (. outfile createNewFile)
        (let [results (get-results (:_id run) results-type)
              fields (get-fields results)
              csv (apply str (map (fn [r] (format-csv-row (map (fn [f] (get r f)) fields)))
                                  results))]
          ;; save into cache file
          (with-open [writer (io/writer outfile)]
            (.write writer (format-csv-row (map name fields)))
            (.write writer csv)))))))
