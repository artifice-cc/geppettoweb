(ns sisyphus.views.run
  (:require [clojure.java.io :as io])
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [geppetto.runs :only
         [get-run list-projects set-project delete-run gather-results-fields]])
  (:use [sisyphus.models.common])
  (:use [sisyphus.config])
  (:use [sisyphus.views.graphs :only [graphs]])
  (:use [sisyphus.views.analyses :only [analyses]])
  (:use [sisyphus.views.parameters :only [parameters-summary]]))

(defn make-run-command
  [run]
  (format (str "lein trampoline run <strong>--action run "
          "--params \"%s/%s\" --nthreads %d --repetitions %d --seed %d</strong>")
     (:problem run) (:name run) (:nthreads run) (:repetitions run) (:seed run)))

(defpartial run-metainfo
  [run]
  [:section
   [:div.page-header
    [:a {:name "metadata"}
     [:h1 "Metadata"]]]
   [:div.row-fluid
    [:div.span12.columns
     [:h2 "Commit message"]
     [:p (link-to (format "https://bitbucket.org/joshuaeckroth/retrospect/changeset/%s"
                     (:commit run))
                  (subs (:commit run) 0 10))
      " / " (:branch run) " @ " (common/date-format (:commitdate run))]]]
   [:div.row-fluid
    [:div.span12.columns
     [:pre (:commitmsg run)]]]
   [:div.row-fluid
    [:div.span12.columns
     [:h2 "Simulation properties"]]]
   [:div.row-fluid
    [:div.span4.columns
     [:dl [:dt "User@host"]
      [:dd (format "%s@%s" (:username run) (:hostname run))]]
     [:dl [:dt "Time"]
      (let [mins (if-not (:endtime run) ""
                         (format "<br/>(~%.0f mins)"
                            (double (/ (- (.getTime (:endtime run))
                                          (.getTime (:starttime run)))
                                       (* 1000 60)))))]
        [:dd (format "%s%s" (common/date-format (:starttime run)) mins)])]]
    [:div.span4.columns
     [:dl [:dt "Reptitions"]
      [:dd (:repetitions run)]]
     [:dl [:dt "Seed"]
      [:dd (:seed run)]]]
    [:div.span4.columns
     [:dl [:dt "Number of threads"]
      [:dd (:nthreads run)]]
     [:dl [:dt "Simulation type"]
      [:dd (if (:comparison run) "comparative" "non-comparative")]]]]
   [:div.row-fluid
    [:div.span12.columns
     [:h2 "Record directory"]
     [:p (:recorddir run)]]]
   [:div.row-fluid
    [:div.span12.columns
     [:h2 "Run command"]
     [:pre (make-run-command run)]]]])

(defpartial run-parameters
  [run]
  [:section
   [:div.page-header
    [:a {:name "parameters"}
     [:h1 "Parameters"]]]
   ;; treat run as params since it has all the right fields
   (parameters-summary run true)])

(defpartial run-project
  [run]
  (let [projects (list-projects)]
    [:section
     [:div.page-header
      [:a {:name "project"}
       [:h1 "Project"]]]
     [:form.form-horizontal {:method "POST" :action "/run/set-project"}
      (hidden-field :runid (:runid run))
      [:div.control-group
       [:label.control-label {:for "project-select"} "Existing project"]
       [:div.controls
        (drop-down :project-select (concat ["New..."] projects) (:project run))]]
      [:div.control-group
       [:label.control-label {:for "new-project"} "New project"]
       [:div.controls (text-field :new-project)]]
      [:div.form-actions [:input.btn.btn-primary {:value "Update" :type "submit"}]]]]))

(defpartial run-delete-run
  [run]
  [:section
   [:div.page-header
    [:h1 "Delete"]]
   (form-to [:post "/run/delete-run"]
            (hidden-field :runid (:runid run))
            [:div.form-actions
             [:input.btn.btn-danger {:value "Delete run" :type "submit"}]])])

(defpage
  [:post "/run/set-project"] {:as project}
  (if (and (= "New..." (:project-select project)) (empty? (:new-project project)))
    (resp/redirect (format "/run/%s" (:runid project)))
    (do
      (set-project (:runid project) (if (= "New..." (:project-select project))
                                      (:new-project project) (:project-select project)))
      (resp/redirect (format "/run/%s#project" (:runid project))))))

(defpage
  [:post "/run/delete-run"] {:as run}
  (common/layout
   "Confirm deletion"
   (common/confirm-deletion "/run/delete-run-confirm" (:runid run)
                            "Are you sure you want to delete the run?")))

(defpage
  [:post "/run/delete-run-confirm"] {:as confirm}
  ;; use :id in confirm map not :runid
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-run (Integer/parseInt (:id confirm)))
      (resp/redirect "/"))
    (resp/redirect (format "/run/%s" (:id confirm)))))

(defpage "/run/:runid" {runid :runid}
  (let [run (get-run runid)
        comparative-fields (gather-results-fields runid :comparative)
        control-fields (gather-results-fields runid :control)]
    (common/layout
     (format "%s/%s run %s" (:problem run) (:name run) runid)
     [:header.jumbotron.subhead
      [:div.row-fluid
       [:h1 (format "%s/%s run %s <small>(%s)</small>"
               (:problem run) (:name run) runid
               (if (:comparison run)
                 "comparative" "non-comparative"))]]]
     [:p (link-to (format "/run/tables/%s" runid)
                  "View tables...")]
     (analyses run)
     (graphs run comparative-fields control-fields)
     (run-parameters run)
     (run-project run)
     (run-metainfo run)
     (run-delete-run run))))
