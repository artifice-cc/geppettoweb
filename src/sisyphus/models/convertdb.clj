(ns sisyphus.models.convertdb
  (:gen-class)
  (:import (java.util Date))
  (:import (java.text SimpleDateFormat))
  (:require [clojure.string :as str])
  (:use [clojure.java.shell :only [sh]])
  (:use [sisyphus.models.common])
  (:use [sisyphus.models.parameters :only [list-parameters get-parameters-by-problem-name]])
  (:use [sisyphus.models.analysis :only [list-analysis]])
  (:use [sisyphus.models.graphs :only [list-graphs]])
  (:use [sisyphus.models.runs :only [list-runs-couch get-summary-results]])
  (:use [korma.core]))

(defn string-or-null
  [s]
  (cond (nil? s) s
        (not (re-find #"\S+" s)) nil
        :else s))

(defn get-paramid
  [problem name rev]
  (:paramid (first (select parameters (where {:name name :problem problem :rev rev})))))

(defn get-runid
  [param-problem param-name paramrev starttime]
  (:runid (first (select runs (where {:paramid (get-paramid param-problem param-name paramrev)
                                      :starttime starttime})))))

(defn convert-params
  [params]
  (let [rev (Integer/parseInt (second (re-matches #"^(\d+)-.*" (:_rev params))))]
    (when (nil? (get-paramid (:problem params) (:name params) rev))
      (println (format "Converting params %s/%s rev %s" (:problem params) (:name params) rev))
      (insert parameters (values [{:comparison (string-or-null (:comparison params))
                                   :control (string-or-null (:control params))
                                   :description (string-or-null (:description params))
                                   :name (:name params)
                                   :problem (:problem params)
                                   :rev rev}])))))

(defn convert-parameters
  []
  (let [{:keys [comparative non-comparative]} (list-parameters)]
    (doseq [params (concat comparative non-comparative)]
      (let [{:keys [start ids]} (:_revisions (get-doc (:_id params)))]
        (doseq [i (range start 0 -1)]
          (convert-params (get-doc (:_id params) (format "%d-%s" i (nth ids (- start i))))))))))

(defn get-analysisid
  [problems name]
  (when (and problems name)
    (let [problems-canonical (str/join "," (sort (str/split problems #",")))]
      (:analysisid (first (select analyses(where {:problems problems-canonical
                                                  :name name})))))))

(defn convert-analysis
  [analysis]
  (when (nil? (get-analysisid (:problem analysis) (:name analysis)))
    (let [problems-canonical (str/join "," (sort (str/split (:problem analysis) #",")))]
      (println (format "Converting analysis %s / %s" problems-canonical (:name analysis)))
      (insert analyses (values [{:problems problems-canonical :name (:name analysis)
                                 :code (:code analysis) :caption (:caption analysis)
                                 :resultstype (:resultstype analysis)}])))))

(defn convert-analyses
  []
  (doseq [analysis (set (apply concat (map second (list-analysis))))]
    (convert-analysis analysis)))

(defn get-graphid
  [problems name]
  (when (and problems name)
    (let [problems-canonical (str/join "," (sort (str/split problems #",")))]
      (:graphid (first (select graphs (where {:problems problems-canonical :name name})))))))

(defn convert-graph
  [graph]
  (when (nil? (get-graphid (:problem graph) (:name graph)))
    (let [problems-canonical (str/join "," (sort (str/split (:problem graph) #",")))]
      (println (format "Converting graph %s / %s" problems-canonical (:name graph)))
      (insert graphs (values [{:problems problems-canonical :name (:name graph)
                               :code (:code graph) :caption (:caption graph)
                               :resultstype (:resultstype graph)
                               :width (:width graph) :height (:height graph)}])))))

(defn convert-graphs
  []
  (doseq [graph (set (apply concat (map second (list-graphs))))]
    (convert-graph graph)))

(defn format-date
  [timestamp]
  (.format (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") (Date. timestamp)))

(defn get-commit-date
  [commit]
  (let [git-output (:out (sh "git" "--git-dir=/home/josh/git/research/retrospect/.git" "show"
                             "--format=raw" commit))
        timestamp (second (re-find #"committer .* (\d+) -0[45]00" git-output))]
    (:out (sh "date" "+%Y-%m-%d %H:%M:%S" (format "--date=@%s" timestamp)))))

(defn convert-run
  [run]
  (let [[_ param-problem param-name] (re-matches #"([^/]+)/(.*)" (:paramsname run))
        paramrev (Integer/parseInt (second (re-matches #"^(\d+)-.*" (:paramsrev run))))
        paramid (get-paramid param-problem param-name paramrev)]
    (when (and (nil? (get-runid param-problem param-name paramrev (format-date (:time run))))
               (not (nil? paramid)))
      (println)
      (println (format "Converting run for %s/%s-%d started on %s"
                  param-problem param-name paramrev (format-date (:time run))))
      (let [runid (:generated_key
                   (insert runs (values [{:paramid paramid
                                          :branch (:branch run)
                                          :commit (:commit run)
                                          :commitmsg (:commit-msg run)
                                          :commitdate (get-commit-date (:commit run))
                                          :database (:database run)
                                          :datadir (:datadir run)
                                          :starttime (format-date (:time run))
                                          :endtime (format-date (:endtime run))
                                          :hostname (:hostname run)
                                          :nthreads (:nthreads run)
                                          :pwd (:pwd run)
                                          :recorddir (:recorddir run)
                                          :repetitions (:repetitions run)
                                          :seed (:seed run)
                                          :username (:username run)
                                          :project (:project run)}])))]
        (doseq [graph-hash (:graphs run)]
          (let [graph (get-doc graph-hash)
                graphid (get-graphid (:problem graph) (:name graph))]
            (insert run-graphs (values [{:runid runid :graphid graphid}]))))
        (doseq [analysis-hash (:analysis run)]
          (let [analysis (get-doc analysis-hash)
                analysisid (get-analysisid (:problem analysis) (:name analysis))]
            (insert run-analyses (values [{:runid runid :analysisid analysisid}]))))
        (doseq [resultstype [:control :comparison :comparative]]
          (doseq [sim-results (get-summary-results run resultstype)]
            (let [results (dissoc sim-results :control-params :comparison-params
                                  :params :simulation)
                  simid (when (not-empty results)
                          (:generated_key
                           (insert simulations
                                   (values [{:runid runid
                                             :controlparams (or (:control-params sim-results)
                                                                (:params sim-results))
                                             :comparisonparams (:comparison-params sim-results)}]))))]
              (when simid
                (doseq [[field val] results]
                  (let [entry {:simid simid
                               :resultstype (name resultstype)
                               :field (name field)}
                        entry-typed (cond (= Double (type val))
                                          (assoc entry :valtype "floatval" :floatval val)
                                          (= Integer (type val))
                                          (assoc entry :valtype "intval" :intval val)
                                          :else
                                          (assoc entry :valtype "strval" :strval val))]
                    (insert results-fields (values [entry-typed]))))))))))))

(defn convert-runs
  []
  (doseq [run (map #(get-doc (:_id %)) (sort-by :_id (list-runs-couch)))]
    (convert-run run)))

(defn -main [& args]
  (convert-analyses)
  (convert-graphs)
  (convert-parameters)
  (convert-runs))
