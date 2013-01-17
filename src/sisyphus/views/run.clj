(ns sisyphus.views.run
  (:require [clojure.java.io :as io])
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [granary.runs :only
         [get-run list-projects set-project delete-run]])
  (:use [sisyphus.models.annotations :only [add-annotation delete-annotation]])
  (:use [sisyphus.models.common])
  (:use [sisyphus.views.graphs :only [graphs]])
  (:use [sisyphus.views.analysis :only [analysis]])
  (:use [sisyphus.views.annotations :only [annotations]])
  (:use [sisyphus.views.parameters :only [parameters-summary]]))

(defn make-run-command
  [run]
  (format (str "lein run -m retrospect.core --action run --params \"%s/%s\" "
               "--nthreads %d --repetitions %d --seed %d")
          (:problem run) (:name run) (:nthreads run)
          (:repetitions run) (:seed run)))

(defpartial run-metainfo
  [run]
  [:section#metadata
   [:div.page-header
    [:a {:name "metadata"}
     [:h2 "Metadata"]]]
   [:div.row
    [:div.span12.columns
     [:h3 "Commit message"]
     [:p (link-to (format "https://bitbucket.org/joshuaeckroth/retrospect/changeset/%s"
                     (:commit run))
                  (subs (:commit run) 0 10))
      " / " (:branch run) " @ " (common/date-format (:commitdate run))]]]
   [:div.row
    [:div.span12.columns
     [:pre (:commitmsg run)]]]
   [:div.row
    [:div.span12.columns
     [:h3 "Simulation properties"]]]
   [:div.row
    [:div.span4.columns
     [:dl [:dt "User@host"]
      [:dd (format "%s@%s" (:username run) (:hostname run))]]
     [:dl [:dt "Time"]
      (let [mins (if-not (:endtime run) ""
                         (format "<br/>(~%.0f mins)"
                            (double (/ (- (.getTime (:endtime run))
                                          (.getTime (:starttime run)))
                                       (* 1000 60)))))]
        [:dd (format "%s%s" (common/date-format (:starttime run)) mins)])]
     [:dl [:dt "Simulation type"]
      [:dd (if (:comparison-params run) "comparative" "non-comparative")]]]
    [:div.span4.columns
     [:dl [:dt "Reptitions"]
      [:dd (:repetitions run)]]
     [:dl [:dt "Seed"]
      [:dd (:seed run)]]
     [:dl [:dt "Number of threads"]
      [:dd (:nthreads run)]]]
    [:div.span4.columns
     [:dl [:dt "Working directory"]
      [:dd (:pwd run)]]
     [:dl [:dt "Data directory"]
      [:dd (:datadir run)]]
     [:dl [:dt "Record directory"]
      [:dd (:recorddir run)]]]]
   [:div.row
    [:div.span12.columns
     [:h3 "Run command"]]]]
  [:div.row
   [:div.span12.columns
    [:pre (make-run-command run)]]])

(defpartial run-parameters
  [run]
  [:section#parameters
   [:div.page-header
    [:a {:name "parameters"}
     [:h2 "Parameters"]]]
   ;; treat run as params since it has all the right fields
   (parameters-summary run true)])

(defpartial run-project
  [run]
  (let [projects (list-projects)]
    [:section#project
     [:div.page-header
      [:a {:name "project"}
       [:h2 "Project"]]]
     [:div.row
      [:div.span12.columns [:p "Choose an existing project, or create a new project."]]]
     [:div.row
      [:div.span12.columns
       (form-to
        [:post "/run/set-project"]
        (hidden-field :runid (:runid run))
        [:div.clearfix
         [:label {:for "project-select"} "Existing project"]
         [:div.input (drop-down :project-select (concat ["New..."] projects)
                                (:project run))]]
        [:div.clearfix
         [:label {:for "new-project"} "New project"]
         [:div.input (text-field :new-project)]]
        [:div.actions [:input.btn.primary {:value "Update" :type "submit"}]])]]]))

(defpartial run-delete-run
  [run]
  [:section#delete
   [:div.page-header
    [:h2 "Delete"]]
   [:div.row
    [:div.span12.columns
     [:p "Delete run and all associated results?"]
     (form-to [:post "/run/delete-run"]
              (hidden-field :runid (:runid run))
              [:div.actions
               [:input.btn.danger {:value "Delete run" :type "submit"}]])]]])

(defpage
  [:post "/run/delete-annotation"] {:as annotation}
  (delete-annotation (:id annotation) (Integer/parseInt (:index annotation)))
  (resp/redirect (format "/run/%s#annotations" (:id annotation))))

(defpage
  [:post "/run/add-annotation"] {:as annotation}
  (add-annotation (:id annotation) (:content annotation))
  (resp/redirect (format "/run/%s#annotations" (:id annotation))))

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

(defn delete-cached-rundata
  [runid]
  (doseq [f (filter #(re-matches (re-pattern (format "%d\\-.*" runid)) (.getName %))
               (file-seq (io/file @cachedir)))]
    (.delete f)))

(defpage
  [:post "/run/delete-run-confirm"] {:as confirm}
  ;; use :id in confirm map not :runid
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-cached-rundata (Integer/parseInt (:id confirm)))
      (delete-run (Integer/parseInt (:id confirm)))
      (resp/redirect "/"))
    (resp/redirect (format "/run/%s" (:id confirm)))))

(defpage "/run/:runid" {runid :runid}
  (let [run (get-run runid)
        comparative-fields []
        control-fields []]
    (common/layout
     (format "%s/%s run %s" (:problem run) (:name run) runid)
     [:div.row [:div.span12.columns
                [:h1 (format "%s/%s run %s <small>(%s)</small>"
                             (:problem run) (:name run) runid
                             (if (:comparison-params run)
                               "comparative" "non-comparative"))]]]
     [:div.row [:div.span12.columns
                [:p (link-to (format "/run/tables/%s" runid)
                             "View tables...")]]]
     #_(analysis run)
     (graphs run comparative-fields control-fields)
     (run-parameters run)
     #_(annotations run "run")
     (run-project run)
     (run-metainfo run)
     (run-delete-run run))))
