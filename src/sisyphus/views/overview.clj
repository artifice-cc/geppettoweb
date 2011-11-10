(ns sisyphus.views.overview
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:require [noir.cookies :as cookies])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.runs :only [list-runs]]))

(defpartial run-table-row
  [run]
  (let [id (:_id run)
        params (get-doc (:paramsid run) (:paramsrev run))]
    [:tr
     [:td (link-to (format "/run/%s" (:_id run)) (subs id 22))]
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
  [:div
   [:div.page-header
    [:h2 problem]]
   [:div.row
    [:div.span16.columns
     (runs-table runs problem)]]])

(defpartial runs-by-problem
  [runs-grouped-problem]
  (map (fn [problem] (runs problem (get runs-grouped-problem problem)))
       (sort (keys runs-grouped-problem))))

(defpartial runs-by-project
  [runs-grouped-project]
  (map (fn [project]
         (let [runs-grouped-problem
               (group-by :problem (get runs-grouped-project project))]
           [:section {:id (format "runs-project-%s" project)}
            [:div.page-header
             [:h1 project]]
            (runs-by-problem runs-grouped-problem)]))
       (sort (keys runs-grouped-project))))

(defpage
  [:post "/set-custom"] {:as custom}
  (cookies/put! (keyword (format "%s-field" (:problem custom))) (:field custom))
  (cookies/put! (keyword (format "%s-func" (:problem custom))) (:func custom))
  (resp/redirect "/"))

(defpage "/" []
  (let [runs-grouped-project (group-by :project (list-runs))]
    (common/layout "Overview" (runs-by-project runs-grouped-project))))
