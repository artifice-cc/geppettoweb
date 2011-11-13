(ns sisyphus.models.simulations
  (:require [clojure.set :as set])
  (:require [clojure.java.io :as io])
  (:require [com.ashafa.clutch :as clutch])
  (:use sisyphus.models.common))

(def dissoc-fields #{:Problem :runid :resultsid :type :_rev :_id
                     :params :control-params :comparison-params :simulation})

(defn get-simulation-fields
  [sim results-type & opts]
  (let [fields (set (keys (first (results-type sim))))]
    (sort (if (some #{:all} opts) fields
              (set/difference fields dissoc-fields)))))

;; Simulation fields are set in the run (so all the run's simulations
;; can share the fields)
(defn set-simulation-fields
  [id fieldstype fields]
  (clutch/with-db db
    (let [sim (get-doc id)
          run (get-doc (:runid sim))]
      (clutch/update-document
       run {(keyword (format "simulation-%s-fields" (name fieldstype))) fields}))))

