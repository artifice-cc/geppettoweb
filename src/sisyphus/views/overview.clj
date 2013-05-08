(ns sisyphus.views.overview
  (:require [clojure.set :as set])
  (:require [clojure.string :as str])
  (:require [sisyphus.views.common :as common])
  (:require [noir.cookies :as cookies])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.analyses :only [analysis-count]])
  (:use [sisyphus.models.graphs :only [graph-count]])
  (:use [geppetto.runs :only [list-runs delete-run]]))

(defpartial run-table-row
  [run show-delete?]
  [:tr
   [:td (link-to (format "/run/%s" (:runid run)) (:runid run))]
   [:td [:div {:style "white-space: nowrap;"} (common/date-format (:starttime run))]]
   [:td (:username run)]
   [:td (link-to (format "/parameters/%d" (:paramid run)) (:name run))]
   [:td (:simcount run)]
   [:td (link-to (format "https://bitbucket.org/joshuaeckroth/retrospect/changeset/%s"
                    (:commit run))
                 (subs (:commit run) 0 10))
    " / " (:branch run)]
   (when show-delete? [:td (check-box "delete[]" false (:runid run))])])

(defpartial runs-table
  [runs problem show-delete?]
  [:table.tablesorter
   [:thead
    [:tr
     [:th "Run"]
     [:th "Time"]
     [:th "User"]
     [:th "Params"]
     [:th "Simulations"]
     [:th "Commit"]
     (when show-delete? [:th "Delete?"])]]
   [:tbody (map #(run-table-row % show-delete?) runs)]])

(defpartial runs
  [problem runs project]
  [:div
   [:a {:name (str (str/replace (or problem "Unknown") #"\W" "_")
                   (str/replace (or project "Unknown") #"\W" "_"))}
    [:h2 problem]]
   (runs-table runs problem true)])

(defpartial runs-by-problem
  [runs-grouped-problem project]
  (map (fn [problem] (runs problem (get runs-grouped-problem problem) project))
       (sort (keys runs-grouped-problem))))

(defpartial runs-by-project
  [runs-grouped-project]
  (map (fn [project]
       (let [project-id (if project (str/replace project #"\W" "_") "Unknown")
             runs-grouped-problem
             (group-by :problem (get runs-grouped-project project))]
         [:section {:id (format "runs-project-%s" project-id)}
          [:div.page-header
           [:a {:name (or project-id "unknown")}
            [:h1 (or project "Unknown project")]]]
          (runs-by-problem runs-grouped-problem project)]))
     (sort (keys runs-grouped-project))))

(defpage
  [:post "/delete-runs"] {:as runs}
  (doseq [runid (:delete runs)]
    (println "Deleting" runid)
    (delete-run runid))
  (resp/redirect "/"))

(defpage "/" []
  (let [runs-grouped-project (group-by :project (list-runs))]
    (common/layout
     "Overview"
     (form-to [:post "/delete-runs"]
              (runs-by-project runs-grouped-project)
              [:div.form-actions
                 [:input.btn.btn-danger
                  {:value "Delete runs" :name "action" :type "submit"}]]))))
