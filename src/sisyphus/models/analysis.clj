(ns sisyphus.models.analysis
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:use [korma.core])
  (:use [granary.models])
  (:use [granary.misc])
  (:use [sisyphus.models.common])
  (:use [sisyphus.models.commonr]))

(defentity analyses
  (pk :analysisid))

(defentity run-analyses
  (table :run_analyses)
  (pk :runanalysisid)
  (belongs-to runs {:fk :runid})
  (has-one analyses {:fk :analysisid}))

(defn analysis-count
  [runid]
  (:count (first (with-db @sisyphus-db
                   (select run-analyses (where {:runid runid})
                           (aggregate (count :runid) :count))))))

(defn list-analysis
  []
  (comment
    (let [all-analysis (:rows (view "analysis-list"))
          problems (set (map (comp first :key) all-analysis))]
      (reduce (fn [m problem]
           (assoc m problem
                  (map :value (filter (fn [a] (= problem (first (:key a))))
                               all-analysis))))
         {} problems))))

(defn get-analysis
  [problem n]
  (comment
    (:value (first (:rows (view "analysis-list" {:key [problem n]}))))))

;; analysis for simulations are set in the run
(defn set-analysis
  [runid analysis run-or-sim]
  (comment
    (let [doc (get-doc runid)]
      (reset-doc-cache runid)
      (clutch/with-db db
        (clutch/update-document doc {(if (= "run" run-or-sim) :analysis
                                         :simulation-analysis) analysis})))))

(defn new-analysis
  [analysis]
  (comment
    (create-doc (assoc analysis :type "analysis"))))

(defn update-analysis
  [analysis]
  (comment
    (let [doc (get-doc (:id analysis))]
      (reset-doc-cache (:id analysis))
      (clutch/with-db db
        (clutch/update-document doc (dissoc analysis :id :_id :_rev))))))

(defn update-analysis-attachment
  [doc output-fname analysis]
  (comment
    (reset-doc-cache (:id doc))
    (try
      (clutch/with-db db
        (clutch/update-attachment doc output-fname
                                  (format "%s-%s" (:_id analysis) (:_rev analysis))
                                  "text/plain"))
      (catch Exception e))))

(defn get-analysis-output
  [doc analysis]
  (comment
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
              (str (:out status) (:err status))))))))

(defn delete-analysis
  [id]
  (comment
    (delete-doc (get-doc id))))
