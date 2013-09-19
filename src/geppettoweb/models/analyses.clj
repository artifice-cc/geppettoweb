(ns geppettoweb.models.analyses
  (:use [clojure.java.shell :only [sh]])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:use [korma.core])
  (:use [fleet])
  (:use [geppetto.models])
  (:use [geppetto.runs])
  (:use [geppetto.misc])
  (:use [geppetto.r])
  (:use [geppettoweb.config])
  (:use [geppettoweb.models.common])
  (:use [geppettoweb.models.commonr]))

(defentity analyses
  (pk :analysisid))

(defentity run-analyses
  (table :run_analyses)
  (pk :runanalysisid)
  (belongs-to runs {:fk :runid})
  (belongs-to analyses {:fk :analysisid}))

(defentity template-analyses
  (table :template_analyses)
  (pk :templateid)
  (belongs-to runs {:fk :runid}))

(defn analysis-count
  [runid]
  (with-db @geppetto-db
    (:count (first (select run-analyses (where {:runid runid})
                           (aggregate (count :runid) :count))))
    (:count (first (select template-analyses (where {:runid runid})
                           (aggregate (count :runid) :count))))))

(defn list-analyses
  []
  (with-db @geppetto-db
    (let [all-analyses (select analyses)
          problems (set (mapcat #(str/split (:problems %) #"\s*,\s*") all-analyses))]
      (reduce (fn [m problem]
           (assoc m problem (filter (fn [a] (some #{problem}
                                         (str/split (:problems a) #"\s*,\s*")))
                               all-analyses)))
         {} problems))))

(defn get-analysis
  [analysisid]
  (first (with-db @geppetto-db (select analyses (where {:analysisid analysisid})))))

(defn get-run-for-template-analysis
  [templateid]
  (:runid (first (with-db @geppetto-db (select template-analyses (where {:templateid templateid}))))))

(defn set-run-analyses
  [runid analysisids]
  (with-db @geppetto-db
    (delete run-analyses (where {:runid runid}))
    (insert run-analyses (values (map (fn [analysisid]
                                      {:runid runid :analysisid analysisid}) analysisids)))))

(defn get-run-analyses
  [runid]
  (map #(dissoc % :runanalysisid :runid :analysisid_2)
     (with-db @geppetto-db
       (select run-analyses (with analyses) (where {:runid runid})))))

(defn get-run-template-analyses
  [runid]
  (with-db @geppetto-db
    (select template-analyses (where {:runid runid}))))

(defn analysis-filename
  [run analysisid templateid]
  (if analysisid
    (format "%s/analysis-%d.txt" (:recorddir run) analysisid)
    (format "%s/template-analysis-%d.txt" (:recorddir run) templateid)))

(defn delete-cached-analyses
  [analysisid]
  (let [runs (list-runs)]
    (doseq [run runs]
      (doseq [f (filter #(re-matches (re-pattern (format "analysis\\-%d\\.txt" analysisid))
                                (.getName %))
                   (file-seq (io/file (:recorddir run))))]
        (.delete f)))))

(defn delete-cached-template-analyses
  [run templateid]
  (.delete (io/file (format "%s/template-analysis-%d.txt" (:recorddir run) templateid))))

(defn get-analysis-output
  [run analysis]
  (let [analysis-fname (analysis-filename run (:analysisid analysis) (:templateid analysis))]
    (if (.exists (io/file analysis-fname))
      (slurp analysis-fname)
      (let [rscript-fname (format "%s/%sanalysis-%d.rscript"
                             (:recorddir run)
                             (if (:templateid analysis)
                               "template-" "")
                             (:analysisid analysis))
            rcode (format "%s # extra funcs
                      load('%s/control.rbin')
                      load('%s/comparison.rbin')
                      load('%s/comparative.rbin')
                      %s # analysis code"
                     extra-funcs
                     (:recorddir run) (:recorddir run) (:recorddir run)
                     (:code analysis))]
        ;; save rcode to file
        (with-open [writer (io/writer rscript-fname)]
          (.write writer rcode))
        ;; run Rscript
        (let [status (sh "/usr/bin/Rscript" rscript-fname)]
          (with-open [writer (io/writer analysis-fname)]
            (.write writer (str (:out status) (:err status))))
          (.delete (io/file rscript-fname))
          (str (:out status) (:err status)))))))

(defn update-analysis
  [analysis]
  (delete-cached-analyses (Integer/parseInt (:analysisid analysis)))
  (with-db @geppetto-db
    (update analyses (set-fields (dissoc analysis :analysisid :action))
            (where {:analysisid (:analysisid analysis)}))))

(defn new-analysis
  [analysis]
  (:generated_key
   (with-db @geppetto-db
     (insert analyses (values [(dissoc analysis :analysisid :action)])))))

(defn delete-analysis
  [analysisid]
  (delete-cached-analyses (Integer/parseInt analysisid))
  (with-db @geppetto-db
    (delete run-analyses (where {:analysisid analysisid}))
    (delete analyses (where {:analysisid analysisid}))))

(defn apply-template
  [run analysis]
  (let [template-file (cond (= (:template analysis) "linear-model")
                            "templates/analysis_template_linear_model.r")
        t (if template-file (fleet [analysis] (slurp template-file)))]
    (if t (assoc analysis :code (str (t analysis)))
        analysis)))

(defn convert-template-analysis-none-fields
  [analysis]
  (let [none-fields #{:xfield :yfield}]
    (reduce (fn [a [k v]] (assoc a k (if (and (none-fields k) (= v "None")) nil v)))
       {} (seq analysis))))

(defn update-template-analysis
  [analysis]
  (let [run (get-run (:runid analysis))]
    (delete-cached-template-analyses run (Integer/parseInt (:templateid analysis)))
    (let [a (apply-template run (convert-template-analysis-none-fields analysis))]
      (with-db @geppetto-db
        (update template-analyses (set-fields (dissoc a :templateid :action))
                (where {:templateid (:templateid a)}))))))

(defn new-template-analysis
  [analysis]
  (:generated_key
   (let [run (get-run (:runid analysis))
         a (apply-template run (convert-template-analysis-none-fields analysis))]
     (with-db @geppetto-db
       (insert template-analyses (values [(dissoc a :templateid :action)]))))))

(defn delete-template-analysis
  [templateid]
  (with-db @geppetto-db
    (let [run (get-run (:runid (first (select template-analyses (where {:templateid templateid})))))]
      (delete-cached-template-analyses run (Integer/parseInt templateid))
      (delete template-analyses (where {:templateid templateid})))))
