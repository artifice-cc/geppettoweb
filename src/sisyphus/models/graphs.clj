(ns sisyphus.models.graphs
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:use [korma.core])
  (:use [granary.models])
  (:use [granary.runs])
  (:use [granary.misc])
  (:use [granary.r])
  (:use [sisyphus.models.common])
  (:use [sisyphus.models.commonr]))

(defentity graphs
  (pk :graphid))

(defentity run-graphs
  (table :run_graphs)
  (pk :rungraphid)
  (belongs-to runs {:fk :runid})
  (belongs-to graphs {:fk :graphid}))

(defn graph-count
  [runid]
  (:count (first (with-db @sisyphus-db
                   (select run-graphs (where {:runid runid})
                           (aggregate (count :runid) :count))))))

(defn default-width-height
  [g]
  (assoc g
    :width (if (nil? (:width g)) 7.0
               (:width g))
    :height (if (nil? (:height g)) 4.0
                (:height g))))

(defn list-graphs
  []
  (with-db @sisyphus-db
    (let [all-graphs (map default-width-height (select graphs))
          problems (set (mapcat #(str/split (:problems %) #"\s*,\s*") all-graphs))]
      (reduce (fn [m problem]
           (assoc m problem (filter (fn [g] (some #{problem}
                                         (str/split (:problems g) #"\s*,\s*")))
                               all-graphs)))
         {} problems))))

(defn get-graph
  [graphid]
  (default-width-height
    (first (with-db @sisyphus-db (select graphs (where {:graphid graphid}))))))

(defn set-run-graphs
  [runid graphids]
  (with-db @sisyphus-db
    (delete run-graphs (where {:runid runid}))
    (insert run-graphs (values (map (fn [graphid] {:runid runid :graphid graphid}) graphids)))))

(defn get-run-graphs
  [runid]
  (map default-width-height
     (map #(dissoc % :rungraphid :runid :graphid_2)
        (with-db @sisyphus-db
          (select run-graphs (with graphs) (where {:runid runid}))))))

(defn new-template-graph
  [template-graph]
  (comment
    (let [tg (create-doc (assoc template-graph :type "template-graph"))
          doc (get-doc (:docid template-graph))]
      (reset-doc-cache (:docid template-graph))
      (clutch/with-db db
        (clutch/update-document doc {:template-graphs
                                     ;; TODO: use conj or something here
                                     [(:_id tg)]})))))

(defn update-graph-attachment
  [doc fname graph ftype theme width height]
  (comment
    (reset-doc-cache (:_id doc))
    (try
      (clutch/with-db db
        (clutch/update-attachment doc fname
                                  (format "%s-%s-%s-%s-%s-%s" (:_id graph) (:_rev graph) ftype
                                     theme width height)
                                  (cond (= "png" ftype) "image/png"
                                        (= "pdf" ftype) "application/pdf"
                                        :else "application/octet-stream")))
      (catch Exception e))))

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

(defn apply-template-bar
  [doc template-graph]
  (comment
    (format "m <- merge(control, comparison, by=c(\"simulation\"))
      result <- data.frame(%s value=c(), se=c())
      for(x_factor in unique(m$%s.y)) {
      %s # fill
      %s # facet_horiz
      %s # facet_vert

      data <- subset(m, %s.y == x_factor %s)

      if(nrow(data) > 0) {
        d <- data.frame(label=\"Diff%s\", value=data$%s.y-data$%s.x)
        dse <- summarySE(d, measurevar=\"value\", groupvars=c(\"label\"))
        result <- rbind(result, data.frame(%s value=dse$value, se=dse$se))
      }
      }
      %s     
      p <- ggplot(result, aes(x=factor(x_factor), y=value%s))
      p <- p + geom_bar(position=position_dodge())
      p <- p + geom_errorbar(aes(ymin=value-se, ymax=value+se),
                                 width=.5, position=position_dodge(.9))
      p <- p + scale_fill_grey()
      p <- p + scale_x_discrete(\"%s\")
      p <- p + scale_y_continuous(\"%s\")
      %s"
       (if (or (not= "None" (:fill template-graph))
               (not= "None" (:facet-horiz template-graph))
               (not= "None" (:facet-vert template-graph)))
         (format "%s, " (str/join ", " (filter identity
                                     [(if (not= "None" (:fill template-graph))
                                        "fill=c()")
                                      (if (not= "None" (:facet-horiz template-graph))
                                        "facet_horiz=c()")
                                      (if (not= "None" (:facet-vert template-graph))
                                        "facet_vert=c()")])))
         "")
       (:x-factor template-graph)
       (if (= "None" (:fill template-graph)) ""
           (format "for(fill in unique(m$%s.y)) {" (:fill template-graph)))
       (if (= "None" (:facet-horiz template-graph)) ""
           (format "for(facet_horiz in unique(m$%s.y)) {" (:facet-horiz template-graph)))
       (if (= "None" (:facet-vert template-graph)) ""
           (format "for(facet_vert in unique(m$%s.y)) {" (:facet-vert template-graph)))
       (:x-factor template-graph)
       (if (or (not= "None" (:fill template-graph))
               (not= "None" (:facet-horiz template-graph))
               (not= "None" (:facet-vert template-graph)))
         (format " & %s"
            (str/join " & " (filter identity
                               [(if (not= "None" (:fill template-graph))
                                  (format "%s.y == fill" (:fill template-graph)))
                                (if (not= "None" (:facet-horiz template-graph))
                                  (format "%s.y == facet_horiz" (:facet-horiz template-graph)))
                                (if (not= "None" (:facet-vert template-graph))
                                  (format "%s.y == facet_vert" (:facet-vert template-graph)))])))
         "")
       (:metric template-graph) (:metric template-graph) (:metric template-graph)
       (if (or (not= "None" (:fill template-graph))
               (not= "None" (:facet-horiz template-graph))
               (not= "None" (:facet-vert template-graph)))
         (format "%s, " (str/join ", " (filter identity
                                     [(if (not= "None" (:fill template-graph))
                                        "fill=fill")
                                      (if (not= "None" (:facet-horiz template-graph))
                                        "facet_horiz=facet_horiz")
                                      (if (not= "None" (:facet-vert template-graph))
                                        "facet_vert=facet_vert")])))
         "")
       (apply str (if (not= "None" (:fill template-graph)) "}" "")
              (if (not= "None" (:facet-horiz template-graph)) "}" "")
              (if (not= "None" (:facet-vert template-graph)) "}" ""))
       (if (not= "None" (:fill template-graph))
         ", fill=factor(fill)" "")
       (:x-axis template-graph)
       (:y-axis template-graph)
       (if (or (not= "None" (:facet_horiz template-graph))
               (not= "None" (:facet_vert template-graph)))
         (format "p <- p + facet_grid(%s ~ %s)"
            (if (not= "None" (:facet_vert template-graph)) "facet_vert" ".")
            (if (not= "None" (:facet_horiz template-graph)) "facet_horiz" ".")))
       )))


(defn apply-template
  [doc template-graph]
  (comment
    (println (cond (= (:template template-graph) "bars")
                   (apply-template-bar doc template-graph)))
    (assoc template-graph :code
           (cond (= (:template template-graph) "bars")
                 (apply-template-bar doc template-graph)))))

(defn graph-filename
  [runid graphid ftype theme width height]
  (format "%s/%d-%d-%s-%.2f-%.2f.%s"
     @cachedir runid graphid theme width height ftype))

(defn delete-cached-graphs
  [graphid]
  (doseq [f (filter #(re-matches (re-pattern (format "\\d+\\-%d\\-.*" graphid)) (.getName %))
               (file-seq (io/file @cachedir)))]
    (.delete f)))

(defn render-graph-file
  [run graph ftype theme width height]
  (let [graph-fname (graph-filename (:runid run) (:graphid graph) ftype theme width height)]
    (if (.exists (io/file graph-fname))
      {:success true}
      (let [rscript-fname (graph-filename (:runid run) (:graphid graph) "rscript"
                                          theme width height)
            g (if (= "template-graph" (:type graph))
                (apply-template run graph) graph)
            rcode (format "library(ggplot2)\nlibrary(grid)
                         %s # extra-funcs
                         %s # themes
                         load('%s/%d-control.rbin')
                         load('%s/%d-comparison.rbin')
                         load('%s/%d-comparative.rbin')
                         p <- ggplot()\n
                         %s # graph code
                         # see: https://github.com/wch/ggplot2/wiki/New-theme-system
                         #p <- p + theme_%s()\n 
                         #%s # scale_colour
                         #%s # scale_fill
                         ggsave(\"%s\", plot = p, dpi = %d, width = %.2f, height = %.2f)"
                     extra-funcs
                     (format "%s\n%s\n%s\n" theme_website theme_paper theme_poster)
                     @cachedir (:runid run) @cachedir (:runid run) @cachedir (:runid run)
                     (:code g) theme
                     (if (not (re-find #"scale_colour" (:code g)))
                       (format "p <- p + scale_colour_manual(values=%s_palette)" theme) "")
                     (if (not (re-find #"scale_fill" (:code g)))
                       (format "p <- p + scale_fill_manual(values=%s_palette)" theme) "" )
                     graph-fname
                     (if (= "png" ftype) 100 600)
                     width height)]
        (results-to-rbin (:runid run) @cachedir)
        ;; save rcode to file
        (with-open [writer (io/writer rscript-fname)]
          (.write writer rcode))
        ;; run Rscript
        (let [status (sh "/usr/bin/Rscript" rscript-fname)]
          (cond (not= 0 (:exit status))
                {:err (str/replace (:err status) r-error-prefix "")}
                (not (. (io/file graph-fname) exists))
                {:err "Resulting file does not exist."}
                :else
                {:success true}))))))

(defn get-graph-download
  [runid graphid ftype theme width height]
  (render-graph-file (get-run runid) (get-graph graphid) ftype theme width height)
  (try (io/input-stream (io/file (graph-filename runid graphid ftype theme width height)))
       (catch Exception _)))

(defn get-graph-png
  [runid graphid]
  (let [graph (get-graph graphid)]
    (get-graph-download runid graphid "png" "website" (:width graph) (:height graph))))

(defn update-graph
  [graph]
  (delete-cached-graphs (Integer/parseInt (:graphid graph)))
  (with-db @sisyphus-db
    (update graphs (set-fields (dissoc graph :graphid :action))
            (where {:graphid (:graphid graph)}))))

(defn new-graph
  [graph]
  (:generated_key
   (with-db @sisyphus-db
     (insert graphs (values [(dissoc graph :graphid :action)])))))

(defn delete-graph
  [graphid]
  (delete-cached-graphs (Integer/parseInt graphid))
  (with-db @sisyphus-db
    (delete run-graphs (where {:graphid graphid}))
    (delete graphs (where {:graphid graphid}))))
