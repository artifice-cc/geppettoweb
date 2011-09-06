(ns sisyphus.views.overview
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.runs :only [problem-fields list-runs]]))

(defpartial run-table-row
  [run]
  (let [id (:id run)
        r (:value run)]
    [:tr
     [:td (link-to (format "/details/%s" id) (common/date-format (:time r)))]
     [:td (:control-strategy r)] [:td (:comparison-strategy r)]
     [:td (:control-count r)] [:td (:comparison-count r)]
     [:td (:comparative-count r)]
     [:td (link-to (format "https://github.com/joshuaeckroth/retrospect/commit/%s" (:commit r))
                   (subs (:commit r) 0 8))]]))

(defpartial runs-table
  [runs]
  [:table [:tr [:th "Time"]
           [:th "Control strategy"] [:th "Comparison strategy"]
           [:th "Control"] [:th "Comparison"] [:th "Comparative"]
           [:th "Commit"]]
   (map run-table-row runs)])

(defpartial runs
  [problem runs]
  (let [fields (problem-fields problem)]
    [:p
     (drop-down :custom fields)
     (runs-table runs)]))

(defpartial runs-by-problem
  [runs-grouped]
  (map (fn [problem] [:p problem (runs problem (runs-grouped problem))])
       (keys runs-grouped)))

(defpage "/" []
  (let [runs-grouped (group-by (comp :problem :value) (list-runs))]
    (runs-by-problem runs-grouped)))
