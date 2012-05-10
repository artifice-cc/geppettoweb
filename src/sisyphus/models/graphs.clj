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

(def theme_minimal
  "theme_minimal <- function (base_size = 12, base_family = \"\") 
   {
       structure(list(axis.line = theme_blank(),
           axis.text.x = theme_text(family = base_family, size = base_size, lineheight = 0.9, vjust = 1, colour = \"black\"), 
           axis.text.y = theme_text(family = base_family, size = base_size, lineheight = 0.9, hjust = 1, colour = \"black\"),
           axis.ticks = theme_segment(colour = \"white\", size = 0.2),
           axis.title.x = theme_text(family = base_family, size = base_size, vjust = 0, colour = \"black\"),
           axis.title.y = theme_text(family = base_family, size = base_size, angle = 90, vjust = 0.25, colour = \"black\"),
           axis.ticks.length = unit(0.2, \"lines\"),
           axis.ticks.margin = unit(0.2, \"lines\"), 
           legend.background = theme_rect(colour = NA),
           legend.key = theme_rect(colour = NA), 
           legend.key.size = unit(1.2, \"lines\"),
           legend.key.height = NA, 
           legend.key.width = NA,
           legend.text = theme_text(family = base_family, size = base_size),
           legend.text.align = NA, 
           legend.title = theme_text(family = base_family, size = base_size, face = \"bold\", hjust = 0),
           legend.title.align = NA, 
           legend.direction = \"vertical\", 
           legend.box = NA,
           panel.background = theme_rect(fill = \"white\", colour = NA),
           panel.border = theme_rect(fill = NA, colour = \"black\"),
           panel.grid.major = theme_line(colour = \"white\", size = 0.2),
           panel.grid.minor = theme_line(colour = \"white\", size = 0.5),
           panel.margin = unit(0.25, \"lines\"), 
           strip.background = theme_rect(fill = NA, colour = NA), 
           strip.text.x = theme_text(family = base_family, size = base_size),
           strip.text.y = theme_text(family = base_family, size = base_size, angle = -90),
           plot.background = theme_rect(colour = NA), 
           plot.title = theme_text(family = base_family, size = base_size * 1.2),
           plot.margin = unit(c(1, 1, 1, 1), \"lines\")),
           panel.border = theme_rect(fill = NA, colour = \"white\"),
           class = \"options\")
   }")

(defn render-graph-file
  [doc graph ftype]
  (reset-doc-cache (:_id doc))
  (if (get-attachment (:_id doc) (format "%s-%s-%s" (:_id graph) (:_rev graph) ftype))
    {:success true}
    (let [csv-fnames (csv-filenames doc)
          ftype-fname (type-filename doc graph ftype)
          tmp-fname (format "%s/%s-%s-%s.rscript"
                            cachedir (:_id doc) (:_id graph) (:_rev graph))
          rcode (format "library(ggplot2)\n%s\n%s\n%s\np <- p + theme_minimal()\n
                         ggsave(\"%s\", plot = p, dpi = 100, width = 7, height = 5)"
                        theme_minimal
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
              {:err (:err status)}
              (not (. (io/file ftype-fname) exists))
              {:err "Resulting file does not exist."}
              :else
              (do (update-graph-attachment doc ftype-fname graph ftype)
                  {:success true}))))))

(defn get-graph-png
  [doc graph]
  (render-graph-file doc graph "png")
  (if-let [f (get-attachment (:_id doc)
                             (format "%s-%s-%s" (:_id graph) (:_rev graph) "png"))]
    (try (io/input-stream f) (catch Exception _))))

(defn get-graph-pdf
  [doc graph]
  (render-graph-file doc graph "pdf")
  (if-let [f (get-attachment (:_id doc)
                             (format "%s-%s-%s" (:_id graph) (:_rev graph) "pdf"))]
    (try (io/input-stream f) (catch Exception _))))

(defn delete-graph
  [id]
  (delete-doc (get-doc id)))
