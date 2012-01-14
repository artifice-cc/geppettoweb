(ns sisyphus.models.graphs
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.results :only [csv-filenames results-to-csv]])
  (:use sisyphus.models.common))

(defn type-filename
  [doc graph ftype]
  (format "%s/%s-%s-%s.%s" cachedir
          (:_id doc) (:_id graph) (:_rev graph) ftype))

(defn list-graphs
  []
  (let [all-graphs (:rows (view "graphs-list"))
        problems (set (map (comp first :key) all-graphs))]
    (reduce (fn [m problem]
              (assoc m problem
                     (map :value (filter (fn [g] (= problem (first (:key g))))
                                         all-graphs))))
            {} problems)))

(defn get-graph
  [problem n]
  (:value (first (:rows (view "graphs-list" {:key [problem n]})))))

;; graphs for simulations are set in the run
(defn set-graphs
  [runid graphs run-or-sim]
  (let [doc (get-doc runid)]
    (reset-doc-cache runid)
    (clutch/with-db db
      (clutch/update-document doc {(if (= "run" run-or-sim) :graphs
                                       :simulation-graphs) graphs}))))

(defn new-graph
  [graph]
  (create-doc (assoc graph :type "graph")))

(defn update-graph
  [graph]
  (let [doc (get-doc (:id graph))]
    (reset-doc-cache (:id graph))
    (clutch/with-db db
      (clutch/update-document doc (dissoc graph :id :_id :_rev)))))

(defn update-graph-attachment
  [doc fname graph ftype]
  (reset-doc-cache (:_id doc))
  (try
    (clutch/with-db db
      (clutch/update-attachment doc fname
                                (format "%s-%s-%s" (:_id graph) (:_rev graph) ftype)
                                (cond (= "png" ftype) "image/png"
                                      (= "pdf" ftype) "application/pdf"
                                      :else "application/octet-stream")))
    (catch Exception e)))

(defn get-graph-file
  [doc graph ftype]
  (reset-doc-cache (:_id doc))
  (if-let [f (get-attachment (:_id doc)
                             (format "%s-%s-%s" (:_id graph) (:_rev graph) ftype))]
    f
    (let [csv-fnames (csv-filenames doc)
          ftype-fname (type-filename doc graph ftype)
          tmp-fname (format "%s/%s-%s-%s.rscript"
                            cachedir (:_id doc) (:_id graph) (:_rev graph))
          rcode (format "library(ggplot2)\n%s\n%s\n
                         ggsave(\"%s\", plot = p, dpi = 100, width = 7, height = 4)"
                        (apply str (map #(format "%s <- read.csv(\"%s\")\n"
                                                 (name %) (get csv-fnames %))
                                        (keys csv-fnames)))
                        (:code graph) ftype-fname)]
      (results-to-csv doc csv-fnames)
      ;; save rcode to file
      (with-open [writer (io/writer tmp-fname)]
        (.write writer rcode))
      ;; run Rscript
      (let [status (sh "/usr/bin/Rscript" tmp-fname)]
        (cond (not= 0 (:exit status))
              status
              (not (. (io/file ftype-fname) exists))
              {:err "Resulting file does not exist."}
              :else (do (update-graph-attachment doc ftype-fname graph ftype)
                        (io/input-stream (io/file ftype-fname))))))))

(defn get-graph-png
  [doc graph]
  (get-graph-file doc graph "png"))

(defn get-graph-pdf
  [doc graph]
  (get-graph-file doc graph "pdf"))

(defn delete-graph
  [id]
  (delete-doc (get-doc id)))
