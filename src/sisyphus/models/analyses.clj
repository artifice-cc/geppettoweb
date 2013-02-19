(ns sisyphus.models.analyses
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:use [korma.core])
  (:use [granary.models])
  (:use [granary.runs])
  (:use [granary.misc])
  (:use [granary.r])
  (:use [sisyphus.config])
  (:use [sisyphus.models.common])
  (:use [sisyphus.models.commonr]))

(defentity analyses
  (pk :analysisid))

(defentity run-analyses
  (table :run_analyses)
  (pk :runanalysisid)
  (belongs-to runs {:fk :runid})
  (belongs-to analyses {:fk :analysisid}))

(defn analysis-count
  [runid]
  (:count (first (with-db @sisyphus-db
                   (select run-analyses (where {:runid runid})
                           (aggregate (count :runid) :count))))))

(defn list-analyses
  []
  (with-db @sisyphus-db
    (let [all-analyses (select analyses)
          problems (set (mapcat #(str/split (:problems %) #"\s*,\s*") all-analyses))]
      (reduce (fn [m problem]
           (assoc m problem (filter (fn [a] (some #{problem}
                                         (str/split (:problems a) #"\s*,\s*")))
                               all-analyses)))
         {} problems))))

(defn get-analysis
  [analysisid]
  (first (with-db @sisyphus-db (select analyses (where {:analysisid analysisid})))))

(defn set-run-analyses
  [runid analysisids]
  (with-db @sisyphus-db
    (delete run-analyses (where {:runid runid}))
    (insert run-analyses (values (map (fn [analysisid]
                                      {:runid runid :analysisid analysisid}) analysisids)))))

(defn get-run-analyses
  [runid]
  (map #(dissoc % :runanalysisid :runid :analysisid_2)
     (with-db @sisyphus-db
       (select run-analyses (with analyses) (where {:runid runid})))))

(defn analysis-filename
  [runid analysisid]
  (format "%s/%d-%d.analysis" @cachedir runid analysisid))

(defn delete-cached-analyses
  [analysisid]
  (doseq [f (filter #(re-matches (re-pattern (format "\\d+\\-%d\\.analysis" analysisid)) (.getName %))
               (file-seq (io/file @cachedir)))]
    (.delete f)))

(defn get-analysis-output
  [run analysis]
  (let [analysis-fname (analysis-filename (:runid run) (:analysisid analysis))]
    (if (.exists (io/file analysis-fname))
      (slurp analysis-fname)
      (let [rscript-fname (format "%s/%d-%d.analysis.rscript"
                             @cachedir (:runid run) (:analysisid analysis))
            rcode (format "%s # extra funcs
                      load('%s/%d-control.rbin')
                      load('%s/%d-comparison.rbin')
                      load('%s/%d-comparative.rbin')
                      %s # analysis code"
                     extra-funcs
                     @cachedir (:runid run) @cachedir (:runid run) @cachedir (:runid run)
                     (:code analysis))]
        (results-to-rbin (:runid run) @cachedir)
        ;; save rcode to file
        (with-open [writer (io/writer rscript-fname)]
          (.write writer rcode))
        ;; run Rscript
        (let [status (sh "/usr/bin/Rscript" rscript-fname)]
          (with-open [writer (io/writer analysis-fname)]
            (.write writer (str (:out status) (:err status))))
          (str (:out status) (:err status)))))))

(defn update-analysis
  [analysis]
  (delete-cached-analyses (Integer/parseInt (:analysisid analysis)))
  (with-db @sisyphus-db
    (update analyses (set-fields (dissoc analysis :analysisid :action))
            (where {:analysisid (:analysisid analysis)}))))

(defn new-analysis
  [analysis]
  (:generated_key
   (with-db @sisyphus-db
     (insert analyses (values [(dissoc analysis :analysisid :action)])))))

(defn delete-analysis
  [analysisid]
  (delete-cached-analyses (Integer/parseInt analysisid))
  (with-db @sisyphus-db
    (delete run-analyses (where {:analysisid analysisid}))
    (delete analyses (where {:analysisid analysisid}))))
