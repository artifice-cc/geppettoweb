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

(defn add-annotation
  [id content]
  (clutch/with-db db (clutch/update-document (get-doc id) #(conj % content) [:annotations])))

(defn delete-annotation
  [id index]
  (let [annotations (:annotations (clutch/get-document id))]
    (clutch/with-db db
      (clutch/update-document
       (get-doc id) {:annotations (concat (take index annotations)
                                          (drop (inc index) annotations))}))))

(def dissoc-fields [:Problem :Step :runid :type :_rev :_id :params :control-params :comparison-params])

(defn get-fields
  [results & opts]
  (if (= 0 (count results)) []
      (sort (apply set/intersection
                   (map (fn [r] (set (keys r)))
                        (map (fn [r] (if (some #{:all} opts) r ;; don't remove fields
                                         (apply dissoc r dissoc-fields))) ;; remove fields
                             results))))))

(defn set-fields
  [id fieldstype fields]
  (clutch/with-db db
    (clutch/update-document
     (get-doc id) {(keyword (format "%s-fields" (name fieldstype))) fields})))

(defn set-graphs
  [id graphs]
  (clutch/with-db db
    (clutch/update-document (get-doc id) {:graphs graphs})))

(defn set-analysis
  [id analysis]
  (clutch/with-db db
    (clutch/update-document (get-doc id) {:analysis analysis})))

(defn get-results
  [id results-type]
  (let [results-ids (get (get-doc id) results-type)]
    (sort-by :Seed (for [i results-ids] (get-doc i)))))

(def cachedir "/tmp")

(defn csv-filenames
  [run]
  (let [results (if (= "comparative" (:paramstype run))
                  [:control :comparison :comparative]
                  [:control])]
    (zipmap results (map #(format "%s/%s-%s.csv" cachedir (:_id run) (name %)) results))))

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
              fields (get-fields results :all)
              csv (apply str (map (fn [r] (format-csv-row (map (fn [f] (get r f)) fields)))
                                  results))]
          ;; save into cache file
          (with-open [writer (io/writer outfile)]
            (.write writer (format-csv-row (map name fields)))
            (.write writer csv)))))))
