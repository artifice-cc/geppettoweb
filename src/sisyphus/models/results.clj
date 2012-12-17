(ns sisyphus.models.results
  (:use [clojure.java.shell :only [sh]])
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

(defn rbin-filenames
  [doc]
  (let [results (if (= "comparative" (:paramstype doc))
                  [:control :comparison :comparative]
                  [:control])]
    (zipmap results (map #(format "%s/%s-%s.bin" cachedir (:_id doc) (name %)) results))))

(defn format-csv-row
  [row]
  ;; add quotes around string data (with "" as per CSV standard, RFC 4180)
  (let [fmt (fn [s] (format "\"%s\"" (str/replace s "\"" "\"\"")))]
    (apply str (concat (interpose "," (map #(cond (= String (type %)) (fmt %)
                                                  (map? %) (fmt (pr-str %))
                                                  :else %)
                                           row))
                       [\newline]))))

(defn results-to-rbin
  [doc]
  (let [rbin-fnames (rbin-filenames doc)
        csv-fnames (csv-filenames doc)]
    ;; write csv files
    (doseq [results-type (keys csv-fnames)]
      (let [outfile (io/file (get csv-fnames results-type))]
        (when (and (not (. outfile exists)))
          (. outfile createNewFile)
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
              (.write writer csv))))))
    ;; write rbin files
    (let [rcode (apply str (map #(format "%s <- read.csv(\"%s\")\nsave(%s, file=\"%s\", compress=FALSE)\n"
                                  (name %) (get csv-fnames %)
                                  (name %) (get rbin-fnames %))
                              (keys csv-fnames)))
          tmp-fname (format "%s/%s-data.rscript" cachedir (:_id doc))]
      (when (some #(not (. (io/file %) exists)) (vals rbin-fnames))
        (with-open [writer (io/writer tmp-fname)]
          (.write writer rcode))
        (sh "/usr/bin/Rscript" tmp-fname)))))
