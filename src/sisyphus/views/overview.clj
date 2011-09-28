(ns sisyphus.views.overview
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:require [noir.cookies :as cookies])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.runs :only
         [problem-fields list-runs summarize-comparative-results]]))

(defpartial run-table-row
  [run summary]
  (let [id (:_id run)]
    [:tr
     [:td (link-to (format "/details/%s" (:_id run)) (subs id 22))]
     [:td (common/date-format (:time run))]
     [:td (link-to (format "/parameters/%s/%s" (:paramsid run) (:paramsrev run))
                   (:paramsname run))]
     [:td (if summary (format "%.2f" summary) "N/A")]
     [:td (:control-count run)] [:td (:comparison-count run)]
     [:td (:comparative-count run)]
     [:td (link-to (format "https://github.com/joshuaeckroth/retrospect/commit/%s" (:commit run))
                   (subs (:commit run) 0 10))
      " @ " (:branch run)]]))

(defpartial runs-table
  [runs problem custom]
  (let [summaries (sort-by second (map (fn [r] [r (summarize-comparative-results (:_id r) custom)])
                                       runs))]
    [:table.tablesorter
     [:thead
      [:tr
       [:th "Run ID"]
       [:th "Time"]
       [:th "Parameters"]
       [:th (format "%s (%s)" (:field custom) (:func custom))]
       [:th "Control"] [:th "Comparison"] [:th "Comparative"]
       [:th "Commit"]]]
     [:tbody
      (map (fn [[run summary]] (run-table-row run summary)) summaries)]]))

(defpartial runs
  [problem runs]
  (let [fields (problem-fields problem)
        custom-field (or (cookies/get (keyword (format "%s-field" problem))) (first fields))
        custom-func (or (cookies/get (keyword (format "%s-func" problem))) "SUM")]
    [:section {:id (format "runs-%s" problem)}
     [:div.page-header
      [:h1 problem]]
     [:div.row
      [:div.span16.columns
       (runs-table runs problem {:field custom-field :func custom-func})]]
     [:div.row
      [:div.span4.columns
       [:h2 "Summarization"]]
      [:div.span12.columns
       (form-to [:post "/set-custom"]
                (hidden-field :problem problem)
                [:div.clearfix
                 [:label {:for "field"} "Field"]
                 [:div.input
                  (drop-down :field fields custom-field)]]
                [:div.clearfix
                 [:label {:for "func"} "Function"]
                 [:div.input
                  (drop-down :func ["AVG" "SUM" "MAX" "MIN"] custom-func)]]
                [:div.actions
                 [:input.btn.primary {:value "Update" :type "submit"}]])]]]))

(defpartial runs-by-problem
  [runs-grouped]
  (map (fn [problem] (runs problem (get runs-grouped problem)))
       (sort (keys runs-grouped))))

(defpage
  [:post "/set-custom"] {:as custom}
  (cookies/put! (keyword (format "%s-field" (:problem custom))) (:field custom))
  (cookies/put! (keyword (format "%s-func" (:problem custom))) (:func custom))
  (resp/redirect "/"))

(defpage "/" []
  (let [runs-grouped (group-by :problem (list-runs))]
    (common/layout "Overview" (runs-by-problem runs-grouped))))
