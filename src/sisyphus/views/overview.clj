(ns sisyphus.views.overview
  (:require [clojure.set :as set])
  (:require [clojure.string :as str])
  (:require [sisyphus.views.common :as common])
  (:require [noir.cookies :as cookies])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.analyses :only [analysis-count]])
  (:use [sisyphus.models.graphs :only [graph-count]])
  (:use [geppetto.runs :only [simulation-count list-runs delete-run]]))

(defpartial run-table-row
  [run]
  [:tr
   [:td (link-to (format "/run/%s" (:runid run)) (:runid run))]
   [:td [:div {:style "white-space: nowrap;"} (common/date-format (:starttime run))]]
   [:td (:username run)]
   [:td (link-to (format "/parameters/%d" (:paramid run)) (:name run))]
   [:td (simulation-count (:runid run))]
   [:td (graph-count (:runid run))]
   [:td (analysis-count (:runid run))]
   [:td (link-to (format "https://bitbucket.org/joshuaeckroth/retrospect/changeset/%s"
                    (:commit run))
                 (subs (:commit run) 0 10))
    " / " (:branch run)]
   [:td (check-box "delete[]" false (:runid run))]])

(defpartial runs-table
  [runs problem]
  [:table.tablesorter
   [:thead
    [:tr
     [:th "Run ID"]
     [:th "Time"]
     [:th "User"]
     [:th "Params"]
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
    [:a {:name (str (str/replace (or problem "Unknown") #"\W" "_")
                    (str/replace (or project "Unknown") #"\W" "_"))}
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
       (let [project-id (if project (str/replace project #"\W" "_") "Unknown")
             runs-grouped-problem
             (group-by :problem (get runs-grouped-project project))]
         [:section {:id (format "runs-project-%s" project-id)}
          [:div.page-header
           [:a {:name (or project-id "unknown")}
            [:h1 (or project "Unknown")]]]
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
    (common/layout "Overview"
                   (form-to [:post "/delete-runs"]
                            (runs-by-project runs-grouped-project)
                            [:div.row
                             [:div.span12.columns
                              [:div.actions
                               [:input.btn.danger
                                {:value "Delete runs" :name "action" :type "submit"}]]]]))))
