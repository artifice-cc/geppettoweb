(ns geppettoweb.models.graphs
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:use [korma db core])
  (:use [fleet])
  (:use [geppetto.models])
  (:use [geppetto.runs])
  (:use [geppetto.misc])
  (:use [geppetto.r])
  (:use [geppettoweb.config])
  (:use [geppettoweb.models.common])
  (:use [geppettoweb.models.commonr]))

(defn graph-count
  [runid]
  (with-db @geppetto-db
    (+ (:count (first (select run-graphs (where {:runid runid})
                              (aggregate (count :runid) :count))))
       (:count (first (select template-graphs (where {:runid runid})
                              (aggregate (count :runid) :count)))))))

(defn default-width-height
  [g]
  (assoc g
    :width (if (nil? (:width g)) 7.0
               (:width g))
    :height (if (nil? (:height g)) 4.0
                (:height g))))

(defn list-graphs
  []
  (with-db @geppetto-db
    (let [all-graphs (map default-width-height (select graphs))
          problems (set (mapcat #(str/split (:problems %) #"\s*,\s*") all-graphs))]
      (reduce (fn [m problem]
                (assoc m problem (filter (fn [g] (some #{problem}
                                                       (str/split (:problems g) #"\s*,\s*")))
                                         all-graphs)))
              {} problems))))

(defn get-graph
  [graphid]
  (with-db @geppetto-db
    (default-width-height
      (first (select graphs (where {:graphid graphid}))))))

(defn get-template-graph
  [templateid]
  (with-db @geppetto-db
    (default-width-height
      (first (select template-graphs (where {:templateid templateid}))))))

(defn get-run-for-template-graph
  [templateid]
  (with-db @geppetto-db
    (:runid (first (select template-graphs (where {:templateid templateid}))))))

(defn set-run-graphs
  [runid graphids]
  (with-db @geppetto-db
    (delete run-graphs (where {:runid runid}))
    (insert run-graphs (values (map (fn [graphid] {:runid runid :graphid graphid}) graphids)))))

(defn get-run-graphs
  [runid]
  (with-db @geppetto-db
    (map default-width-height
         (map #(dissoc % :rungraphid :runid :graphid_2)
              (select run-graphs (with graphs) (where {:runid runid}))))))

(defn get-run-template-graphs
  [runid]
  (with-db @geppetto-db
    (map default-width-height
         (select template-graphs (where {:runid runid})))))

(def r-error-prefix
  "Loading required package: reshape
Loading required package: plyr

Attaching package: ‘reshape’

The following object(s) are masked from ‘package:plyr’:

    rename, round_any

Loading required package: grid
Loading required package: proto")

(defn graph-filename
  [run graphid templateid ftype theme width height]
  (if graphid
    (format "%s/graph-%d-%s-%.2f-%.2f.%s"
       (:recorddir run) graphid theme width height ftype)
    (format "%s/template-graph-%d-%s-%.2f-%.2f.%s"
       (:recorddir run) templateid theme width height ftype)))

(defn delete-cached-graphs
  [graphid]
  (let [runs (list-runs)]
    (doseq [run runs]
      (doseq [f (filter #(re-matches (re-pattern (format "graph-%d\\-.*" graphid))
                                (.getName %))
                   (file-seq (io/file (:recorddir run))))]
        (.delete f)))))

(defn delete-cached-template-graphs
  [run templateid]
  (doseq [f (filter #(re-matches (re-pattern (format "template\\-graph\\-%d\\-.*" templateid))
                            (.getName %))
               (file-seq (io/file (:recorddir run))))]
    (.delete f)))

(defn apply-theme
  [theme]
  (let [template-file (cond (= theme "website")
                            "templates/graph_theme_website.r"
                            (= theme "paper")
                            "templates/graph_theme_paper.r"
                            (= theme "poster")
                            "templates/graph_theme_poster.r")
        t (if template-file (fleet [] (slurp template-file)))]
    (if t (str (t)) "")))

(defn render-graph-file
  [run graph ftype theme width height]
  (let [graph-fname (graph-filename run (:graphid graph) (:templateid graph)
                                    ftype theme width height)]
    (if (.exists (io/file graph-fname))
      {:success true}
      (let [rscript-fname (graph-filename run (:graphid graph) (:templateid graph)
                                          "rscript" theme width height)
            rcode (format "library(ggplot2)
                      library(grid)
                      %s # extra-funcs
                      %s # theme
                      load('%s/control.rbin')
                      load('%s/comparison.rbin')
                      load('%s/comparative.rbin')
                      p <- ggplot()
                      %s # graph code
                      p <- p + theme_custom() # load theme
                      %s # scale_colour
                      %s # scale_fill
                      ggsave(\"%s\", plot = p, dpi = %d, width = %.2f, height = %.2f)"
                     extra-funcs
                     (apply-theme theme)
                     (:recorddir run) (:recorddir run) (:recorddir run)
                     (:code graph)
                     (if (not (re-find #"scale_colour" (:code graph)))
                       "p <- p + scale_colour_manual(values=custom_palette)" "")
                     (if (not (re-find #"scale_fill" (:code graph)))
                       "p <- p + scale_fill_manual(values=custom_palette)" "")
                     graph-fname
                     (if (= "png" ftype) 100 600)
                     width height)]
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
                (do (.delete (io/file rscript-fname))
                    {:success true})))))))

(defn get-graph-download
  [runid graphid templateid ftype theme width height]
  (let [run (get-run runid)
        graph (if graphid (get-graph graphid) (get-template-graph templateid))]
    (render-graph-file (get-run runid) graph ftype theme width height)
    (try (io/input-stream (io/file (graph-filename run graphid templateid
                                                   ftype theme width height)))
         (catch Exception _))))

(defn get-graph-png
  [runid graphid templateid]
  (let [graph (if graphid (get-graph graphid) (get-template-graph templateid))]
    (get-graph-download runid graphid templateid
                        "png" "website" (:width graph) (:height graph))))

(defn update-graph
  [graph]
  (with-db @geppetto-db
    (delete-cached-graphs (Integer/parseInt (:graphid graph)))
    (update graphs (set-fields (dissoc graph :graphid :action))
            (where {:graphid (:graphid graph)}))))

(defn new-graph
  [graph]
  (with-db @geppetto-db
    (:generated_key (insert graphs (values [(dissoc graph :graphid :action)])))))

(defn delete-graph
  [graphid]
  (with-db @geppetto-db
    (delete-cached-graphs (Integer/parseInt graphid))
    (delete run-graphs (where {:graphid graphid}))
    (delete graphs (where {:graphid graphid}))))

(defn apply-template
  [run graph]
  (let [template-file (cond (= (:template graph) "bars")
                            "templates/graph_template_bars.r"
                            (= (:template graph) "bars-comparative")
                            "templates/graph_template_bars_comparative.r"
                            (= (:template graph) "line")
                            "templates/graph_template_line.r"
                            (= (:template graph) "line-comparative")
                            "templates/graph_template_line_comparative.r"
                            (= (:template graph) "density")
                            "templates/graph_template_density.r"
                            (= (:template graph) "histogram")
                            "templates/graph_template_histogram.r"
                            (= (:template graph) "points")
                            "templates/graph_template_points.r")
        t (if template-file (fleet [graph] (slurp template-file)))]
    (if t (assoc graph :code (str (t graph)))
        graph)))

(defn convert-template-graph-none-fields
  [graph]
  (let [none-fields #{:xfield :yfield :fill :color :linetype :shape :facethoriz :facetvert}]
    (reduce (fn [g [k v]] (assoc g k (if (and (none-fields k) (= v "None")) nil v)))
       {} (seq graph))))

(defn update-template-graph
  [graph]
  (with-db @geppetto-db
    (let [run (get-run (:runid graph))]
      (delete-cached-template-graphs run (Integer/parseInt (:templateid graph)))
      (let [g (apply-template run (convert-template-graph-none-fields graph))]
        (update template-graphs (set-fields (dissoc g :templateid :action))
                (where {:templateid (:templateid g)}))))))

(defn new-template-graph
  [graph]
  (with-db @geppetto-db
    (:generated_key
     (let [run (get-run (:runid graph))
           g (apply-template run (convert-template-graph-none-fields graph))]
       (insert template-graphs (values [(dissoc g :templateid :action)]))))))

(defn delete-template-graph
  [templateid]
  (with-db @geppetto-db
    (let [run (get-run (:runid (first (select template-graphs (where {:templateid templateid})))))]
      (delete-cached-template-graphs run (Integer/parseInt templateid))
      (delete template-graphs (where {:templateid templateid})))))
