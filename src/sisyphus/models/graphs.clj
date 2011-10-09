(ns sisyphus.models.graphs
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.runs :only [get-results get-fields]])
  (:use sisyphus.models.common))

(def cachedir "/tmp")

(defn csv-filenames
  [run]
  (zipmap [:control :comparison :comparative]
          (map #(format "%s/%s-%s.csv" cachedir (:_id run) (name %))
               [:control :comparison :comparative])))

(defn png-filename
  [run graph]
  (format "%s/%s-%s-%s.png" cachedir
          (:_id run) (:_id graph) (:_rev graph)))

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

(defn list-graphs
  []
  (let [all-graphs (:rows (view "graphs-list"))
        problems (set (map (comp first :key) all-graphs))]
    (reduce (fn [m problem] (assoc m problem
                                   (map :value (filter (fn [g] (= problem (first (:key g))))
                                                       all-graphs))))
            {} problems)))

(defn get-graph
  [problem n]
  (:value (first (:rows (view "graphs-list" {:key [problem n]})))))

(defn new-graph
  [graph]
  (create-doc (assoc graph :type "graph")))

(defn update-graph
  [graph]
  (clutch/with-db db
    (clutch/update-document (get-doc (:id graph)) (dissoc graph :_id :_rev))))

(defn get-graph-png
  [run graph]
  (if-let [png (get-attachment (:_id run) (format "%s-%s" (:_id graph) (:_rev graph)))]
    png
    (let [csv-fnames (csv-filenames run)
          png-fname (png-filename run graph)
          rcode (format "library(ggplot2)\n%s\n%s\nggsave(\"%s\", plot = p, dpi = 100, width = 7, height = 4)"
                        (apply str (map #(format "%s <- read.csv(\"%s\")\n" (name %) (get csv-fnames %))
                                        (keys csv-fnames)))
                        (:code graph) png-fname)]
      (results-to-csv run csv-fnames)
      ;; save rcode to file
      (with-open [writer (io/writer (format "%s/tmp.rscript" cachedir))]
        (.write writer rcode))
      ;; run Rscript
      (let [status (sh "/usr/bin/Rscript" (format "%s/tmp.rscript" cachedir))]
        (cond (not= 0 (:exit status))
              status
              (not (. (io/file png-fname) exists))
              {:err "Resulting file does not exist."}
              :else (do
                      (clutch/with-db db
                        (clutch/update-attachment run png-fname
                                                  (format "%s-%s" (:_id graph) (:_rev graph))
                                                  "image/png"))
                      (io/input-stream (io/file png-fname))))))))
