(ns sisyphus.models.analysis
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.results :only [csv-filenames results-to-csv]])
  (:use sisyphus.models.common))

(defn list-analysis
  []
  (let [all-analysis (:rows (view "analysis-list"))
        problems (set (map (comp first :key) all-analysis))]
    (reduce (fn [m problem]
              (assoc m problem
                     (map :value (filter (fn [a] (= problem (first (:key a))))
                                         all-analysis))))
            {} problems)))

(defn get-analysis
  [problem n]
  (:value (first (:rows (view "analysis-list" {:key [problem n]})))))

;; analysis for simulations are set in the run
(defn set-analysis
  [runid analysis run-or-sim]
  (clutch/with-db db
    (clutch/update-document (get-doc runid) {(if (= "run" run-or-sim) :analysis
                                                 :simulation-analysis) analysis})))

(defn new-analysis
  [analysis]
  (create-doc (assoc analysis :type "analysis")))

(defn update-analysis
  [analysis]
  (clutch/with-db db
    (clutch/update-document (get-doc (:id analysis)) (dissoc analysis :id :_id :_rev))))

(defn update-analysis-attachment
  [doc output-fname analysis]
  (try
    (clutch/with-db db
      (clutch/update-attachment doc output-fname
                                (format "%s-%s" (:_id analysis) (:_rev analysis))
                                "text/plain"))
    (catch Exception e)))

(defn get-analysis-output
  [doc analysis]
  (if-let [output (get-attachment
                   (:_id doc) (format "%s-%s" (:_id analysis) (:_rev analysis)))]
    (slurp output)
    (let [csv-fnames (csv-filenames doc)
          tmp-fname (format "%s/%s-%s-%s.rscript"
                            cachedir (:_id doc) (:_id analysis) (:_rev analysis))
          output-fname (format "%s/%s-%s-%s.output"
                               cachedir (:_id doc) (:_id analysis) (:_rev analysis))
          rcode (format "%s\n%s\n"
                        (apply str (map #(format "%s <- read.csv(\"%s\")\n"
                                                 (name %) (get csv-fnames %))
                                        (keys csv-fnames)))
                        (:code analysis))]
      (results-to-csv doc csv-fnames)
      ;; save rcode to file
      (with-open [writer (io/writer tmp-fname)]
        (.write writer rcode))
      ;; run Rscript
      (let [status (sh "/usr/bin/Rscript" tmp-fname)]
        (do (with-open [writer (io/writer output-fname)]
              (.write writer (str (:out status) (:err status))))
            (update-analysis-attachment doc output-fname analysis)
            (str (:out status) (:err status)))))))

(defn delete-analysis
  [id]
  (delete-doc (get-doc id)))
