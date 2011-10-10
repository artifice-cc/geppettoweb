(ns sisyphus.models.analysis
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.runs :only
         [get-results get-fields csv-filenames results-to-csv cachedir]])
  (:use sisyphus.models.common))

(defn list-analysis
  []
  (let [all-analysis (:rows (view "analysis-list"))
        problems (set (map (comp first :key) all-analysis))]
    (reduce (fn [m problem] (assoc m problem
                                   (map :value (filter (fn [a] (= problem (first (:key a))))
                                                       all-analysis))))
            {} problems)))

(defn get-analysis
  [problem n]
  (:value (first (:rows (view "analysis-list" {:key [problem n]})))))

(defn new-analysis
  [analysis]
  (create-doc (assoc analysis :type "analysis")))

(defn update-analysis
  [analysis]
  (clutch/with-db db
    (clutch/update-document (get-doc (:id analysis)) (dissoc analysis :id :_id :_rev))))

(defn update-analysis-attachment
  [runid output-fname analysis]
  (try
    (clutch/with-db db
      (clutch/update-attachment (get-doc runid) output-fname
                                (format "%s-%s" (:_id analysis) (:_rev analysis))
                                "text/plain"))
    (catch Exception e (update-analysis-attachment runid output-fname analysis))))

(defn get-analysis-output
  [runid analysisid analysisrev]
  (if-let [output (get-attachment runid (format "%s-%s" analysisid analysisrev))]
    (slurp output)
    (let [run (get-doc runid)
          analysis (get-doc analysisid analysisrev)
          csv-fnames (csv-filenames run)
          tmp-fname (format "%s/%s-%s-%s.rscript"
                            cachedir runid (:_id analysis) (:_rev analysis))
          output-fname (format "%s/%s-%s-%s.output"
                               cachedir runid (:_id analysis) (:_rev analysis))
          rcode (format "%s\n%s\n"
                        (apply str (map #(format "%s <- read.csv(\"%s\")\n" (name %) (get csv-fnames %))
                                        (keys csv-fnames)))
                        (:code analysis))]
      (results-to-csv run csv-fnames)
      ;; save rcode to file
      (with-open [writer (io/writer tmp-fname)]
        (.write writer rcode))
      ;; run Rscript
      (let [status (sh "/usr/bin/Rscript" tmp-fname)]
        (do (with-open [writer (io/writer output-fname)]
              (.write writer (str (:out status) (:err status))))
            (update-analysis-attachment runid output-fname analysis)
            (str (:out status) (:err status)))))))
