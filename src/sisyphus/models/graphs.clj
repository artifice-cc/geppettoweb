(ns sisyphus.models.graphs
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.runs :only [get-results get-fields]]))

(def local-couchdb "http://localhost:5984/retrospect")

(def cachedir "/tmp")
(def outdir "/home/josh/research/sisyphus/resources/public/img/graphs")
(def url "/img/graphs")

(defn csv-filename
  [run graph]
  (format "%s/%s-%s-%s.csv" cachedir (:_id run)
          (:_rev run) (name (:results-type graph))))

(defn png-filename
  [run graph]
  (format "%s/%s-%s-%s-%s.png" outdir
          (:_id run) (:_rev run) (:_id graph) (:_rev graph)))

(defn format-csv-row
  [row]
  ;; add quotes around string data
  (apply str (concat (interpose "," (map #(if (= String (type %)) (format "\"%s\"" %) %) row))
                     [\newline])))

(defn results-to-csv
  [run csv-fname graph]
  (let [outfile (io/file csv-fname)]
    (when (. outfile createNewFile)
      (let [results (get-results (:_id run) (:results-type graph))
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
        problems (set (map (comp first :key) all-graphs))]
    (reduce (fn [m problem] (assoc m problem
                                   (map :value (filter (fn [g] (= problem (first (:key g))))
                                                       all-graphs))))
            {} problems)))

(defn new-graph
  [graph]
  (clutch/with-db local-couchdb
    (clutch/create-document (assoc graph :type "graph"))))

(defn update-graph
  [graph]
  (clutch/with-db local-couchdb
    (clutch/update-document (clutch/get-document (:id graph)) (dissoc graph :_id :_rev))))

(defn get-graph-png
  [run graph]
  (let [csv-fname (csv-filename run graph)
        png-fname (png-filename run graph)
        rcode (format "library(ggplot2)\n%s <- read.csv(\"%s\")\n%s\nggsave(\"%s\", plot = p, dpi = 100, width = 7, height = 4)"
                      (name (:results-type graph)) csv-fname (:code graph) png-fname)]
    (do (results-to-csv run csv-fname graph)
        (if (. (io/file png-fname) exists)
          (str/replace png-fname outdir url)
          (do
            ;; save rcode to file
            (with-open [writer (io/writer (format "%s/tmp.rscript" cachedir))]
              (.write writer rcode))
            ;; run Rscript
            (let [status (sh "/usr/bin/Rscript" (format "%s/tmp.rscript" cachedir))]
              (when (not= 0 (:exit status)) (prn status)))
            (when (. (io/file png-fname) exists)
              (str/replace png-fname outdir url)))))))
