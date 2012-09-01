(ns sisyphus.models.runs
  (:require [clojure.set :as set])
  (:require [clojure.string :as str])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.claims :only [list-claims remove-claim-association]])
  (:use sisyphus.models.common))

(defn list-runs
  []
  (map :value (:rows (view "runs-list"))))

(defn delete-run
  [id]
  (let [run (get-doc id)
        claims (apply concat (vals (list-claims run)))]
    (doseq [c claims]
      (remove-claim-association {:claim (:_id c) :runid id}))
    (doseq [r (:results run)]
      (let [doc (get-doc r)]
        (when doc (delete-doc doc))))
    (delete-doc run)))

(defn list-projects
  []
  (sort (set (filter identity (map (comp :project :value)
                                   (:rows (view "runs-list")))))))

(defn set-project
  [id project]
  (let [doc (get-doc id)]
    (reset-doc-cache id)
    (clutch/with-db db
      (clutch/update-document doc {:project project}))))

(def dissoc-fields #{:Problem :runid :resultsid :type :_rev :_id
                     :params :control-params :comparison-params :simulation})

(defn get-simulation-fields
  [sim results-type & opts]
  (let [fields (apply set/union (map #(set (keys %)) (results-type sim)))]
    (sort (if (some #{:all} opts) fields
              (set/difference fields dissoc-fields)))))

(defn get-summary-fields
  [run results-type & opts]
  ;; the first map of results should have the same fields as all the others
  (get-simulation-fields (get-doc (first (:results run))) results-type))

(defn get-fields
  [run results-type & opts]
  (if (some #{:all} opts)
    ;; get all possible fields, not just those activated; used by
    ;; get-summary-results below for CSV output
    (set (map keyword (get-summary-fields run results-type)))
    ;; get only activated fields
    (set (map keyword (get run (keyword (format "%s-fields" (name results-type))) #{})))))

(defn set-fields
  [id fields results-type]
  (let [doc (get-doc id)]
    (reset-doc-cache id)
    (clutch/with-db db
      (clutch/update-document doc {(keyword (format "%s-fields" (name results-type)))
                                   (map keyword fields)}))))

(defn summarize-sim-results
  [sim results-type fields]
  (concat
   (if (:params (last (get sim results-type)))
     [(:params (last (get sim results-type)))]
     [(:control-params (last (get sim results-type)))
      (:comparison-params (last (get sim results-type)))])
   (map #(get (last (get sim results-type)) %) fields)))

(defn get-summary-results
  "Get results with all fields or only those requested."
  ([run results-type]
     (let [sims (get-many-docs (:results run))
           fields (get-fields run results-type :all)]
       (map (fn [sim] (zipmap (concat [:simulation]
                                   (if (:params (first (get sim results-type)))
                                     [:params] [:control-params :comparison-params])
                                   fields)
                           (concat [(:simulation (first (get sim results-type)))]
                                   (summarize-sim-results sim results-type fields))))
          sims)))
  ([run results-type fields]
     (let [sims (get-many-docs (:results run))]
       (map (fn [sim]
            (zipmap
             (concat
              (if (:params (first (get sim results-type)))
                [:params] [:control-params :comparison-params])
              fields)
             (summarize-sim-results sim results-type fields)))
          sims))))
