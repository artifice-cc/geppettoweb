(ns sisyphus.models.simulations
  (:require [clojure.set :as set])
  (:require [clojure.java.io :as io])
  (:require [com.ashafa.clutch :as clutch])
  (:use sisyphus.models.common))

(def dissoc-fields #{:Problem :runid :resultsid :type :_rev :_id
                     :params :control-params :comparison-params})

(defn get-simulation-fields
  [sim fieldstype & opts]
  (let [fields (set (keys (first (fieldstype sim))))]
    (sort (if (some #{:all} opts) fields
              (set/difference fields dissoc-fields)))))

(defn set-simulation-fields
  [id fieldstype fields]
  (clutch/with-db db
    (clutch/update-document
     (get-doc id) {(keyword (format "%s-fields" (name fieldstype))) fields})))

