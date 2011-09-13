(ns sisyphus.models.graphs
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.runs :only [get-results get-fields]]))

(def local-couchdb "http://localhost:5984/retrospect")

(def cachedir "/tmp")

(defn csv-filename
  [run results-type]
  (format "%s/%s-%s-%s.csv" cachedir (:_id run)
          (:_rev run) (name results-type)))

(defn png-filename
  [run results-type graph-name]
  (format "%s/%s-%s-%s-%s.png" cachedir (:_id run)
          (:_rev run) (name results-type) graph-name))

(defn format-csv-row
  [row]
  ;; add quotes around string data
  (apply str (concat (interpose "," (map #(if (= String (type %)) (format "\"%s\"" %) %) row))
                     [\newline])))

(defn results-to-csv
  [run results-type]
  (let [outfile (io/file (csv-filename run results-type))]
    (when (. outfile createNewFile)
      (let [results (get-results (:_id run) results-type)
            fields (get-fields results)
            csv (apply str (map (fn [r] (format-csv-row (map (fn [f] (get r f)) fields)))
                                results))]
        ;; save into cache file
        (with-open [writer (io/writer outfile)]
          (.write writer (format-csv-row (map name fields)))
          (.write writer csv))))))

(defn list-graphs
  []
  (let [all-graphs
        (:rows
         (clutch/with-db local-couchdb
           (clutch/ad-hoc-view
            (clutch/with-clj-view-server
              {:map (fn [doc]
                      (when (= "graph" (:type doc))
                        [[[(:problem doc) (:name doc)] doc]]))}))))
        problems (sort (set (map (comp first :key) all-graphs)))]
    (reduce (fn [m problem] (assoc m problem
                                   (map :value (filter (fn [g] (= problem (first (:key g))))
                                                       all-graphs))))
            {} problems)))

(defn new-graph
  [graph]
  (clutch/with-db local-couchdb
    (clutch/create-document (assoc graph :type "graph"))))

(defn get-graph
  [run results-type graph-name]
  (let [csv (csv-filename run results-type)
        png (png-filename run results-type graph-name)
        graph "p <- ggplot(comparative) + geom_point(aes(x=Threshold, y=IncreaseAccuracy))"
        rcode (format "library(ggplot2)\n%s <- read.csv(\"%s\")\n%s\nggsave(\"%s\", plot = p, dpi = 100, width = 4, height = 4)"
                      (name results-type) csv graph png)]
    ;; save rcode to file
    (with-open [writer (io/writer (format "%s/tmp.rscript" cachedir))]
      (.write writer rcode))
    ;; run Rscript
    (sh "/usr/bin/Rscript" (format "%s/tmp.rscript" cachedir))
    (when (. (io/file png) exists)
      png)))
