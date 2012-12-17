(ns sisyphus.models.graphs
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.results :only [rbin-filenames results-to-rbin]])
  (:use sisyphus.models.common)
  (:use sisyphus.models.commonr))

(defn type-filename
  [doc graph ftype]
  (format "%s/%s-%s-%s.%s" cachedir
          (:_id doc) (:_id graph) (:_rev graph) ftype))

(defn list-graphs
  []
  (let [all-graphs (:rows (view "graphs-list"))
        problems (set (mapcat #(str/split (first (:key %)) #"\s*,\s*") all-graphs))]
    (reduce (fn [m problem]
              (assoc m problem
                     (map :value (filter (fn [g] (some #{problem}
                                            (str/split (first (:key g)) #"\s*,\s*")))
                                         all-graphs))))
            {} problems)))

(defn get-graph
  [problem n]
  (first #(= n (:name %)) (get (list-graphs) problem)))

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
  [doc fname graph ftype theme width height]
  (reset-doc-cache (:_id doc))
  (try
    (clutch/with-db db
      (clutch/update-attachment doc fname
                                (format "%s-%s-%s-%s-%s-%s" (:_id graph) (:_rev graph) ftype
                                   theme width height)
                                (cond (= "png" ftype) "image/png"
                                      (= "pdf" ftype) "application/pdf"
                                      :else "application/octet-stream")))
    (catch Exception e)))

(def theme_website
  "website_palette <- c(\"#3465a4\", \"#2e3436\", \"#f57900\")
   theme_website <- function (base_size = 12, base_family = \"\") {
   theme_grey(base_size=base_size, base_family=base_family) %+replace%
     theme(
       axis.line = theme_blank(),
       axis.text.x = theme_text(family = base_family, size = base_size * 0.8, lineheight = 0.9, vjust = 1), 
       axis.text.y = theme_text(family = base_family, size = base_size * 0.8, lineheight = 0.9, hjust = 1),
       axis.ticks = theme_segment(colour = \"black\", size = 0.2),
       axis.title.x = theme_text(family = base_family, size = base_size, vjust = 0),
       axis.title.y = theme_text(family = base_family, size = base_size, angle = 90, vjust = 0.5),
       axis.ticks.length = unit(0.3, \"lines\"),
       axis.ticks.margin = unit(0.5, \"lines\"),
       legend.background = theme_rect(colour = NA), 
       legend.margin = unit(0.2, \"cm\"),
       legend.key = theme_rect(colour = NA), 
       legend.key.size = unit(1.2, \"lines\"),
       legend.text = theme_text(family = base_family, size = base_size * 0.8),
       legend.title = theme_text(family = base_family, size = base_size * 0.8, face = \"bold\", hjust = 0),
       legend.justification = \"center\",
       panel.background = theme_rect(fill = \"white\", colour = NA),
       panel.border = theme_rect(fill = NA, colour = \"grey90\"),
       panel.grid.major = theme_line(colour = \"grey90\", size = 0.2),
       panel.grid.minor = theme_line(colour = \"grey98\", size = 0.5),
       panel.margin = unit(0.25, \"lines\"), 
       strip.background = theme_rect(fill = NA, colour = NA), 
       strip.text.x = theme_text(family = base_family, size = base_size * 0.8),
       strip.text.y = theme_text(family = base_family, size = base_size * 0.8, angle = -90),
       plot.background = theme_rect(colour = NA), 
       plot.title = theme_text(family = base_family, size = base_size * 1.2),
       plot.margin = unit(c(1, 1, 0.5, 0.5), \"lines\"))
   }")

(def theme_paper
  "paper_palette <- c(\"#555555\", \"#999999\", \"#aaaaaa\")
   theme_paper <- function (base_size = 12, base_family = \"\") {
   structure(list(
       axis.line = theme_blank(),
       axis.text.x = theme_text(family = base_family, size = base_size * 0.8, lineheight = 0.9, vjust = 1), 
       axis.text.y = theme_text(family = base_family, size = base_size * 0.8, lineheight = 0.9, hjust = 1),
       axis.ticks = theme_segment(colour = \"black\", size = 0.2),
       axis.title.x = theme_text(family = base_family, size = base_size, vjust = 0),
       axis.title.y = theme_text(family = base_family, size = base_size, angle = 90, vjust = 0.5),
       axis.ticks.length = unit(0.3, \"lines\"),
       axis.ticks.margin = unit(0.5, \"lines\"),
       legend.background = theme_rect(colour = NA), 
       legend.margin = unit(0.2, \"cm\"),
       legend.key = theme_rect(colour = NA), 
       legend.key.size = unit(1.2, \"lines\"),
       legend.text = theme_text(family = base_family, size = base_size * 0.8),
       legend.title = theme_text(family = base_family, size = base_size * 0.8, face = \"bold\", hjust = 0),
       legend.justification = \"center\",
       legend.box = NA,
       panel.background = theme_rect(fill = \"white\", colour = NA),
       panel.border = theme_rect(fill = NA, colour = \"grey90\"),
       panel.grid.major = theme_line(colour = \"grey90\", size = 0.2),
       panel.grid.minor = theme_line(colour = \"grey98\", size = 0.5),
       panel.margin = unit(0.25, \"lines\"), 
       strip.background = theme_rect(fill = NA, colour = NA), 
       strip.text.x = theme_text(family = base_family, size = base_size * 0.8),
       strip.text.y = theme_text(family = base_family, size = base_size * 0.8, angle = -90),
       plot.background = theme_rect(colour = NA), 
       plot.title = theme_text(family = base_family, size = base_size * 1.2),
       plot.margin = unit(c(1, 1, 0.5, 0.5), \"lines\")),
     class = \"options\")
   }")

(def theme_poster
  "poster_palette <- c(\"#3465a4\",  \"#2e3436\", \"#f57900\")
   theme_poster <- function (base_size = 12, base_family = \"\") {
   structure(list(
       axis.line = theme_blank(),
       axis.text.x = theme_text(colour = \"#2e3436\", family = base_family, size = base_size * 0.8, lineheight = 0.9, vjust = 1), 
       axis.text.y = theme_text(colour = \"#2e3436\", family = base_family, size = base_size * 0.8, lineheight = 0.9, hjust = 1),
       axis.ticks = theme_segment(colour = \"#2e3436\", size = 0.2),
       axis.title.x = theme_text(colour = \"#2e3436\", family = base_family, size = base_size, vjust = 0),
       axis.title.y = theme_text(colour = \"#2e3436\", family = base_family, size = base_size, angle = 90, vjust = 0.5),
       axis.ticks.length = unit(0.3, \"lines\"),
       axis.ticks.margin = unit(0.5, \"lines\"),
       legend.background = theme_rect(colour = NA), 
       legend.margin = unit(0.2, \"cm\"),
       legend.key = theme_rect(colour = NA), 
       legend.key.size = unit(1.2, \"lines\"),
       legend.text = theme_text(colour = \"#2e3436\", family = base_family, size = base_size * 0.8),
       legend.title = theme_text(colour = \"#2e3436\", family = base_family, size = base_size * 0.8, face = \"bold\", hjust = 0),
       legend.justification = \"center\",
       legend.box = NA,
       panel.background = theme_rect(fill = \"white\", colour = NA),
       panel.border = theme_rect(fill = NA, colour = \"#2e3436\", size = 1),
       panel.grid.major = theme_line(colour = \"#d3d7cf\", size = 0.2),
       panel.grid.minor = theme_line(colour = \"#eeeeec\", size = 0.5),
       panel.margin = unit(0.25, \"lines\"), 
       strip.background = theme_rect(fill = NA, colour = NA),
       strip.text.x = theme_text(colour = \"#2e3436\", family = base_family, size = base_size * 0.8),
       strip.text.y = theme_text(colour = \"#2e3436\", family = base_family, size = base_size * 0.8, angle = -90),
       plot.background = theme_rect(colour = NA), 
       plot.title = theme_text(colour = \"#2e3436\", family = base_family, size = base_size * 1.2),
       plot.margin = unit(c(1, 1, 0.5, 0.5), \"lines\")),
     class = \"options\")
   }")

(def r-error-prefix
  "Loading required package: reshape
Loading required package: plyr

Attaching package: ‘reshape’

The following object(s) are masked from ‘package:plyr’:

    rename, round_any

Loading required package: grid
Loading required package: proto")

(defn render-graph-file
  [doc graph ftype theme width height]
  (reset-doc-cache (:_id doc))
  (if (get-attachment (:_id doc) (format "%s-%s-%s-%s-%s-%s" (:_id graph) (:_rev graph) ftype
                                    theme width height))
    {:success true}
    (let [rbin-fnames (rbin-filenames doc)
          ftype-fname (type-filename doc graph ftype)
          tmp-fname (format "%s/%s-%s-%s.rscript"
                       cachedir (:_id doc) (:_id graph) (:_rev graph))
          rcode (format "library(ggplot2)\nlibrary(grid)\n%s\n%s\n%s\n
                         p <- ggplot()\n
                         %s\n
                         # see: https://github.com/wch/ggplot2/wiki/New-theme-system
                         #p <- p + theme_%s()\n 
                         %s\n
                         %s\n
                         ggsave(\"%s\", plot = p, dpi = %d, width = %d, height = %d)"
                   extra-funcs
                   (format "%s\n%s\n%s\n" theme_website theme_paper theme_poster)
                   (apply str (map #(format "load(\"%s\")\n" (get rbin-fnames %))
                                 (keys rbin-fnames)))
                   (:code graph) theme
                   (if (not (re-find #"scale_colour" (:code graph)))
                     (format "p <- p + scale_colour_manual(values=%s_palette)" theme) "")
                   (if (not (re-find #"scale_fill" (:code graph)))
                     (format "p <- p + scale_fill_manual(values=%s_palette)" theme) "" )
                   ftype-fname
                   (if (= "png" ftype) 100 600)
                   (Integer/parseInt width)
                   (Integer/parseInt height))]
      (results-to-rbin doc)
      ;; save rcode to file
      (with-open [writer (io/writer tmp-fname)]
        (.write writer rcode))
      ;; run Rscript
      (let [status (sh "/usr/bin/Rscript" tmp-fname)]
        (cond (not= 0 (:exit status))
              {:err (str/replace (:err status) r-error-prefix "")}
              (not (. (io/file ftype-fname) exists))
              {:err "Resulting file does not exist."}
              :else
              (do (update-graph-attachment doc ftype-fname graph ftype theme width height)
                  {:success true}))))))

(defn get-graph-png
  [doc graph]
  (render-graph-file doc graph "png" "website" (:width graph "7") (:height graph "4"))
  (if-let [f (get-attachment (:_id doc)
                             (format "%s-%s-%s-%s-%s-%s" (:_id graph) (:_rev graph) "png"
                                "website" (:width graph "7") (:height graph "4")))]
    (try (io/input-stream f) (catch Exception _))))

(defn get-graph-download
  [doc graph theme width height ftype]
  (render-graph-file doc graph ftype theme width height)
  (if-let [f (get-attachment (:_id doc)
                             (format "%s-%s-%s-%s-%s-%s" (:_id graph) (:_rev graph) ftype
                                theme width height))]
    (try (io/input-stream f) (catch Exception _))))

(defn delete-graph
  [id]
  (delete-doc (get-doc id)))
