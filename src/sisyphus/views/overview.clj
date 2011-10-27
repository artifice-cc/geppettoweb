(ns sisyphus.views.overview
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:require [noir.cookies :as cookies])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.runs :only
         [problem-fields list-runs summarize-comparative-results]]))

(defpartial run-table-row
  [run]
  (let [id (:_id run)
        params (get-doc (:paramsid run) (:paramsrev run))]
    [:tr
     [:td (link-to (format "/details/%s" (:_id run)) (subs id 22))]
     [:td (common/date-format (:time run))]
     [:td (:username run)]
     [:td (link-to (format "/parameters/%s/%s" (:paramsid run) (:paramsrev run))
                   (format "%s (%s)" (:paramsname run)
                           (if (= "comparative" (:paramstype run)) "c" "nc")))]
     [:td (:count run)]
     [:td (:graph-count run)]
     [:td (:analysis-count run)]
     [:td (link-to (format "https://bitbucket.org/joshuaeckroth/retrospect/changeset/%s" (:commit run))
                   (subs (:commit run) 0 10))
      " @ " (:branch run)]]))

(defpartial runs-table
  [runs problem]
  [:table.tablesorter
   [:thead
    [:tr
     [:th "Run ID"]
     [:th "Time"]
     [:th "User"]
     [:th "Params (c/nc)"]
     [:th "Sims"]
     [:th "Graphs"]
     [:th "Analysis"]
     [:th "Commit"]]]
   [:tbody (map run-table-row runs)]])

(defpartial runs
  [problem runs]
  (let [fields (problem-fields problem)]
    [:section {:id (format "runs-%s" problem)}
     [:div.page-header
      [:h1 problem]]
     [:div.row
      [:div.span16.columns
       (runs-table runs problem)]]]))

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
