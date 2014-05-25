(ns geppettoweb.views.overview
  (:use [geppettoweb.views.common :only [gurl]])
  (:require [clojure.set :as set])
  (:require [clojure.string :as str])
  (:require [geppettoweb.views.common :as common])
  (:require [ring.util.response :as resp])
  (:use compojure.core hiccup.def hiccup.element hiccup.form hiccup.util)
  (:use [geppettoweb.models.analyses :only [analysis-count]])
  (:use [geppettoweb.models.graphs :only [graph-count]])
  (:use [geppetto.runs :only [list-runs delete-run]]))

(defhtml run-table-row
  [run show-delete?]
  [:tr
   [:td (link-to (gurl (format "/run/%s" (:runid run))) (:runid run))]
   [:td [:div {:style "white-space: nowrap;"} (common/date-format (:starttime run))]]
   [:td (:username run)]
   [:td (link-to (gurl (format "/parameters/%d" (:paramid run))) (:name run))]
   [:td (:simcount run)]
   [:td (link-to (format "https://bitbucket.org/joshuaeckroth/retrospect/changeset/%s"
                    (:commit run))
                 (subs (:commit run) 0 10))
    " / " (:branch run)]
   (when show-delete? [:td (check-box "delete[]" false (:runid run))])])

(defhtml runs-table
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

(defhtml runs
  [problem runs project]
  [:div
   [:h2 [:a {:name (str (str/replace (or problem "Unknown") #"\W" "_")
                        (str/replace (or project "Unknown") #"\W" "_"))}]
    problem]
   (runs-table runs problem true)])

(defhtml runs-by-problem
  [runs-grouped-problem project]
  (map (fn [problem] (runs problem (get runs-grouped-problem problem) project))
       (sort (keys runs-grouped-problem))))

(defhtml runs-by-project
  [runs-grouped-project]
  (map (fn [project]
         (let [project-id (if project (str/replace project #"\W" "_") "Unknown")
               runs-grouped-problem
               (group-by :problem (get runs-grouped-project project))]
           [:section {:id (format "runs-project-%s" project-id)}
            [:div.page-header
             [:h1 [:a {:name (or project-id "unknown")}]
              (or project "Unknown project")]]
            (runs-by-problem runs-grouped-problem project)]))
       (sort (keys runs-grouped-project))))

(defn overview []
  (let [runs-grouped-project (group-by :project (list-runs))]
    (common/layout
     "Overview"
     (form-to [:post (gurl "/delete-runs")]
              (runs-by-project runs-grouped-project)
              [:div.form-actions
                 [:input.btn.btn-danger
                  {:value "Delete runs" :name "action" :type "submit"}]]))))

(defroutes overview-routes
  (POST "/delete-runs" [:as {delete :params}]
    (do (doseq [runid (:delete delete)]
             (delete-run runid))
        (resp/redirect (gurl "/"))))
  (GET "/" [] (overview)))
