(ns sisyphus.models.results
  (:require [clojure.string :as str])
  (:require [clojure.java.io :as io])
  (:use [sisyphus.models.runs :only [get-summary-results]])
  (:use sisyphus.models.common))

(defn csv-filenames
  [doc]
  (let [results (if (= "comparative" (:paramstype doc))
                  [:control :comparison :comparative]
                  [:control])]
    (zipmap results (map #(format "%s/%s-%s.csv" cachedir (:_id doc) (name %)) results))))

(defn format-csv-row
  [row]
  ;; add quotes around string data (with "" as per CSV standard, RFC 4180)
  (let [fmt (fn [s] (format "\"%s\"" (str/replace s "\"" "\"\"")))]
    (apply str (concat (interpose "," (map #(cond (= String (type %)) (fmt %)
                                                  (map? %) (fmt (pr-str %))
                                                  :else %)
                                           row))
                       [\newline]))))

(defn results-to-csv
  [doc csv-fnames]
  (doseq [results-type (keys csv-fnames)]
    (let [outfile (io/file (get csv-fnames results-type))]
      (when (and (not (. outfile exists))) (. outfile createNewFile)
            (let [results (if (= "run" (:type doc))
                            ;; for a run
                            (get-summary-results doc results-type)
                            ;; for a simulation
                            (get doc results-type))
                  fields (sort (keys (first results)))
                  csv (apply str (map (fn [r] (format-csv-row
                                               (map (fn [f] (get r f)) fields)))
                                      results))]
              ;; save into cache file
              (with-open [writer (io/writer outfile)]
                (.write writer (format-csv-row (map name fields)))
                (.write writer csv)))))))
