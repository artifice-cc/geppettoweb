(ns sisyphus.views.overview
  (:require [clojure.set :as set])
  (:require [clojure.string :as str])
  (:require [sisyphus.views.common :as common])
  (:require [noir.cookies :as cookies])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.runs :only [list-runs delete-run]]))

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
      " @ " (:branch run)]
     [:td (check-box "delete[]" false id)]]))

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
     [:th "Commit"]
     [:th "Delete?"]]]
   [:tbody (map run-table-row runs)]])

(defpartial runs
  [problem runs project]
  [:div
   [:div.page-header
    [:a {:name (if (and problem project)
                 (str (str/replace problem #"\W" "_")
                      (str/replace project #"\W" "_")) "")}
     [:h2 problem]]]
   [:div.row
    [:div.span12.columns
     (runs-table runs problem)]]])

(defpartial runs-by-problem
  [runs-grouped-problem project]
  (map (fn [problem] (runs problem (get runs-grouped-problem problem) project))
       (sort (keys runs-grouped-problem))))

(defpartial runs-by-project
  [runs-grouped-project]
  (map (fn [project]
         (let [project-id (str/replace project #"\W" "_")
               runs-grouped-problem
               (group-by :problem (get runs-grouped-project project))]
           [:section {:id (format "runs-project-%s" project-id)}
            [:div.page-header
             [:a {:name project-id}
              [:h1 project]]]
            (runs-by-problem runs-grouped-problem project)]))
       (sort (keys runs-grouped-project))))

(defpage
  [:post "/delete-runs"] {:as runs}
  (doseq [id (:delete runs)]
    (println "Deleting" id)
    (delete-run id))
  (resp/redirect "/"))

(defpage "/" []
  (let [runs-grouped-project (group-by :project (list-runs))]
    (common/layout "Overview"
                   (form-to [:post "/delete-runs"]
                            (runs-by-project runs-grouped-project)
                            [:div.row
                             [:div.span12.columns
                              [:div.actions
                               [:input.btn.danger
                                {:value "Delete runs" :name "action" :type "submit"}]]]]))))
