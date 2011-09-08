(ns sisyphus.views.overview
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:require [noir.cookies :as cookies])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.runs :only
         [problem-fields list-runs summarize-comparative-results]]))

(defpartial run-table-row
  [run problem custom]
  (let [id (:id run)
        r (:value run)
        ;; why must a macro be called this way?!
        summary (eval (summarize-comparative-results problem custom))]
    [:tr
     [:td (link-to (format "/details/%s" id) (common/date-format (:time r)))]
     [:td (:control-strategy r)] [:td (:comparison-strategy r)]
     [:td (format "%.2f" ((comp double :value first :rows) summary))]
     [:td (:control-count r)] [:td (:comparison-count r)]
     [:td (:comparative-count r)]
     [:td (link-to (format "https://github.com/joshuaeckroth/retrospect/commit/%s" (:commit r))
                   (subs (:commit r) 0 8))]]))

(defpartial runs-table
  [runs problem custom]
  [:table [:tr [:th "Time"]
           [:th "Control strategy"] [:th "Comparison strategy"]
           [:th (format "%s (%s,%s)" (:field custom) (:order custom) (:func custom))]
           [:th "Control"] [:th "Comparison"] [:th "Comparative"]
           [:th "Commit"]]
   (map #(run-table-row % problem custom) runs)])

(defpartial runs
  [problem runs]
  (let [fields (problem-fields problem)
        custom-field (or (cookies/get (keyword (format "%s-field" problem))) (first fields))
        custom-order (or (cookies/get (keyword (format "%s-order" problem))) "ASC")
        custom-func (or (cookies/get (keyword (format "%s-func" problem))) "SUM")]
    [:p
     (form-to [:post "/set-custom"]
              (hidden-field :problem problem)
              (drop-down :field fields custom-field)
              (drop-down :order ["ASC" "DESC"] custom-order)
              (drop-down :func ["AVG" "SUM" "MAX" "MIN"] custom-func)
              (submit-button "Update"))
     (runs-table runs problem {:field custom-field :order custom-order :func custom-func})]))

(defpartial runs-by-problem
  [runs-grouped]
  (map (fn [problem] [:p problem (runs problem (get runs-grouped problem))])
       (keys runs-grouped)))

(defpage
  [:post "/set-custom"] {:as custom}
  (cookies/put! (keyword (format "%s-field" (:problem custom))) (:field custom))
  (cookies/put! (keyword (format "%s-order" (:problem custom))) (:order custom))
  (cookies/put! (keyword (format "%s-func" (:problem custom))) (:func custom))
  (resp/redirect "/"))

(defpage "/" []
  (let [runs-grouped (group-by (comp :problem :value) (list-runs))]
    (runs-by-problem runs-grouped)))
