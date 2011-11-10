(ns sisyphus.models.runs
  (:require [clojure.set :as set])
  (:require [clojure.string :as str])
  (:require [clojure.java.io :as io])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.claims :only [list-claims remove-claim-association]])
  (:use [sisyphus.models.simulations :only [get-simulation-fields]])
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
    (doseq [r (:results run)]
      (delete-doc (get-doc r)))
    (delete-doc run)))

(defn set-graphs
  [id graphs]
  (clutch/with-db db
    (clutch/update-document (get-doc id) {:graphs graphs})))

(defn set-analysis
  [id analysis]
  (clutch/with-db db
    (clutch/update-document (get-doc id) {:analysis analysis})))

(defn list-projects
  []
  (sort (set (filter identity (map (comp :project :value)
                                   (:rows (view "runs-list")))))))

(defn set-project
  [id project]
  (clutch/with-db db
    (clutch/update-document (get-doc id) {:project project})))

(defn get-summary-fields
  [run results-type & opts]
  (get-simulation-fields (get-doc (first (:results run))) results-type opts))

(defn get-fields-funcs
  [run results-type]
  (let [ffs (get run (keyword (format "%s-fields-funcs" (name results-type))))]
    (filter #(not= "N/A" (second %))
            (map (fn [field] [field (get ffs field)]) (keys ffs)))))

(defn set-fields-funcs
  [id fields results-type]
  (clutch/with-db db
    (clutch/update-document (get-doc id)
                            {(keyword (format "%s-fields-funcs" (name results-type)))
                             fields})))

(defn format-summary-fields
  [fields-funcs]
  (map (fn [[field func]] (format "%s (%s)" (name field) func)) fields-funcs))

(defn get-summary-results
  [run results-type fields-funcs]
  (let [sims (map get-doc (:results run))]
    (map (fn [sim]
           (zipmap
            (concat
             ["Simulation"]
             (if (:params (first (get sim results-type)))
               [:params] [:control-params :comparison-params])
             (format-summary-fields fields-funcs))
            (concat
             [(format "<a href=\"/simulation/%s\">%s</a>"
                      (:_id sim) (subs (:_id sim) 22))]
             (if (:params (first (get sim results-type)))
               [(:params (first (get sim results-type)))]
               [(:control-params (first (get sim results-type)))
                (:comparison-params (first (get sim results-type)))])
             (for [[field func] fields-funcs]
               (let [vals (filter number? (map field (get sim results-type)))]
                 (if (empty? vals) (get (first (get sim results-type)) field)
                     (cond (= func "sum")
                           (reduce + 0 vals)
                           (= func "avg")
                           (double (/ (reduce + 0 vals) (count vals)))
                           (= func "min")
                           (apply min vals)
                           (= func "max")
                           (apply max vals)
                           :else (reduce + 0 vals))))))))
         sims)))

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
  (let [fmt (fn [s] (format "\"%s\"" (str/replace s "\"" "\\\"")))]
    (apply str (concat (interpose "," (map #(cond (= String (type %)) (fmt %)
                                                  (map? %) (fmt (pr-str %))
                                                  :else %)
                                           row))
                       [\newline]))))

(defn results-to-csv
  [run csv-fnames]
  (doseq [results-type (keys csv-fnames)]
    (let [outfile (io/file (get csv-fnames results-type))]
      (when (. outfile createNewFile)
        (let [results (get-summary-results (:_id run) results-type)
              fields (get-summary-fields results :all)
              csv (apply str (map (fn [r] (format-csv-row (map (fn [f] (get r f)) fields)))
                                  results))]
          ;; save into cache file
          (with-open [writer (io/writer outfile)]
            (.write writer (format-csv-row (map name fields)))
            (.write writer csv)))))))
