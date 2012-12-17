(ns sisyphus.models.analysis
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.results :only [rbin-filenames results-to-rbin]])
  (:use sisyphus.models.common)
  (:use sisyphus.models.commonr))

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
  (let [doc (get-doc runid)]
    (reset-doc-cache runid)
    (clutch/with-db db
      (clutch/update-document doc {(if (= "run" run-or-sim) :analysis
                                       :simulation-analysis) analysis}))))

(defn new-analysis
  [analysis]
  (create-doc (assoc analysis :type "analysis")))

(defn update-analysis
  [analysis]
  (let [doc (get-doc (:id analysis))]
    (reset-doc-cache (:id analysis))
    (clutch/with-db db
      (clutch/update-document doc (dissoc analysis :id :_id :_rev)))))

(defn update-analysis-attachment
  [doc output-fname analysis]
  (reset-doc-cache (:id doc))
  (try
    (clutch/with-db db
      (clutch/update-attachment doc output-fname
                                (format "%s-%s" (:_id analysis) (:_rev analysis))
                                "text/plain"))
    (catch Exception e)))

(defn get-analysis-output
  [doc analysis]
  (reset-doc-cache (:id doc))
  (if-let [output (get-attachment
                   (:_id doc) (format "%s-%s" (:_id analysis) (:_rev analysis)))]
    (slurp output)
    (let [rbin-fnames (rbin-filenames doc)
          tmp-fname (format "%s/%s-%s-%s.rscript"
                       cachedir (:_id doc) (:_id analysis) (:_rev analysis))
          output-fname (format "%s/%s-%s-%s.output"
                          cachedir (:_id doc) (:_id analysis) (:_rev analysis))
          rcode (format "%s\n%s\n%s\n"
                   extra-funcs
                   (apply str (map #(format "load(\"%s\")\n" (get rbin-fnames %))
                                 (keys rbin-fnames)))
                   (:code analysis))]
      (results-to-rbin doc)
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
