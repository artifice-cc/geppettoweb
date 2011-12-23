(ns sisyphus.views.run
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:require [clojure.contrib.string :as str])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.runs :only
         [get-summary-results get-summary-fields
          get-fields-funcs set-fields-funcs
          format-summary-fields list-projects set-project delete-run]])
  (:use [sisyphus.models.annotations :only [add-annotation delete-annotation]])
  (:use [sisyphus.models.claims :only [claim-select-options list-claims]])
  (:use [sisyphus.views.fields :only [field-selects]])
  (:use [sisyphus.views.claims :only
         [claim-summary]])
  (:use [sisyphus.views.graphs :only [graphs]])
  (:use [sisyphus.views.analysis :only [analysis]])
  (:use [sisyphus.views.annotations :only [annotations]])
  (:use [sisyphus.views.parameters :only [parameters-summary]])
  (:use [sisyphus.views.results :only
         [results-table paired-results-table]]))

(defn make-run-command
  [run]
  (format (str "lein run -m retrospect.core --action run --params \"%s\" "
               "--database \"%s\" --nthreads %d --repetitions %d --seed %d")
          (:paramsname run) (:database run) (:nthreads run)
          (:repetitions run) (:seed run)))

(defpartial run-metainfo
  [run]
  [:section#metadata
   [:div.page-header
    [:a {:name "metadata"}
     [:h2 "Metadata"]]]
   [:div.row
    [:div.span4.columns
     [:h3 "Commit message"]
     [:p (link-to (format "https://bitbucket.org/joshuaeckroth/retrospect/changeset/%s"
                          (:commit run))
                  (subs (:commit run) 0 10))
      " @ " (:branch run)]]
    [:div.span12.columns
     [:pre (:commit-msg run)]]]
   [:div.row
    [:div.span4.columns
     [:h3 "Simulation properties"]]
    [:div.span4.columns
     [:dl [:dt "User@host"]
      [:dd (format "%s@%s" (:username run) (:hostname run))]]
     [:dl [:dt "Time"]
      (let [mins (if-not (:endtime run) ""
                         (format "<br/>(~%.0f mins)"
                                 (double (/ (- (:endtime run) (:time run))
                                            (* 1000 60)))))]
        [:dd (format "%s%s" (common/date-format (:time run)) mins)])]
     [:dl [:dt "Simulation type"]
      [:dd (:paramstype run)]]]
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
    [:div.span4.columns
     [:h3 "Run command"]]
    [:div.span12.columns
     [:pre (make-run-command run)]]]])

(defpartial run-parameters
  [run]
  (let [params (get-doc (:paramsid run) (:paramsrev run))]
    [:section#parameters
     [:div.page-header
      [:a {:name "parameters"}
       [:h2 "Parameters"]]]
     (if (not= (keys params) [:revs])
       (parameters-summary params)
       [:p "Error in getting parameters."])]))

(defpartial run-fields-form
  [run results-type fields fields-funcs]
  (form-to
   [:post "/run/set-fields"]
   (hidden-field :id (:_id run))
   (hidden-field :results-type results-type)
   [:div.row
    [:div.span4.columns
     [:p [:b [:a.fields_checkboxes_header "Select active fields..."]]]]]
   [:div.fields_checkboxes
    [:div.row
     [:div.span4.columns "&nbsp;"]
     (field-selects run results-type fields fields-funcs)]
    [:div.row
     [:div.span4.columns "&nbsp;"]
     [:div.span12.columns
      [:div.actions
       [:input.btn.primary {:value "Update" :type "submit"}]]]]]))

(defpartial run-comparative-results-table
  [run comparative-fields]
  (let [fields-funcs (get-fields-funcs run :comparative)
        on-fields (concat ["Simulation"] (format-summary-fields fields-funcs))
        results (get-summary-results run :comparative fields-funcs)]
    [:section#comparative-results
     [:div.page-header
      [:a {:name "comparative-results"}
       [:h2 "Comparative results"]]]
     (results-table results on-fields)
     (run-fields-form run :comparative comparative-fields fields-funcs)]))

(defpartial run-paired-results-table
  [run control-fields]
  (let [fields-funcs (get-fields-funcs run :paired)
        on-fields (concat ["Simulation"] (format-summary-fields fields-funcs))
        control-results (get-summary-results run :control fields-funcs)
        comparison-results (get-summary-results run :comparison fields-funcs)]
    [:section#paired-results
     [:div.page-header
      [:a {:name "control-comparison-results"}
       [:h2 "Control/comparison results"]]]
     (paired-results-table control-results comparison-results on-fields)
     (run-fields-form run :paired control-fields fields-funcs)]))

(defpartial run-non-comparative-results-table
  [run control-fields]
  (let [fields-funcs (get-fields-funcs run :non-comparative)
        on-fields (concat ["Simulation"] (format-summary-fields fields-funcs))
        results (get-summary-results run :control fields-funcs)]
    [:section#non-comparative-results
     [:div.page-header
      [:a {:name "results"}
       [:h2 "Results"]]]
     (results-table results on-fields)
     (run-fields-form run :non-comparative control-fields fields-funcs)]))

(defpartial claim-association-form
  [run]
  (let [claim-opts (claim-select-options run)]
    (if (not-empty claim-opts)
      (form-to
       [:post "/claims/add-association"]
       [:div
        (hidden-field :runid (:_id run))
        [:div.row
         [:div.span4.columns
          [:h3 "New association"]]
         [:div.span12.columns
          [:div.clearfix
           [:label {:for "claim"} "Claim"]
           [:div.input
            (drop-down :claim claim-opts)]]
          [:div.clearfix
           [:label {:for "comment"} "Comment"]
           [:div.input
            [:textarea.xxlarge {:id "comment" :name "comment"}]
            [:span.help-block "Describe how this run provides support
                          for or against the claim."]]]
          [:div.clearfix
           [:div.actions
            [:input.btn.primary {:value "Associate" :type "submit"}]]]]]]))))

(defpartial run-claims
  [run]
  (let [run-claims (list-claims run)]
    [:section#claims
     [:div.page-header
      [:a {:name "claims"}
       [:h2 "Claims"]]]
     [:div.row
      [:div.span4.columns
       [:h3 "Associated claims"]]
      [:div.span12.columns
       (if (and (empty? (:verified run-claims))
                (empty? (:unverified run-claims)))
         [:p "No claims."]
         [:div
          (if (not-empty (:unverified run-claims))
            [:h4 "Unverified"])
          (for [c (:unverified run-claims)]
            (claim-summary c))
          (if (not-empty (:verified run-claims))
            [:h4 "Verified"])
          (for [c (:verified run-claims)]
            (claim-summary c))])]]
     (claim-association-form run)]))

(defpartial run-overview-notes
  [run]
  [:section#overview
   [:div.page-header
    [:a {:name "notes"}
     [:h2 "Overview notes"]]]
   [:div.row
    [:div.span4.columns "&nbsp;"]
    [:div.span12.columns {:style "max-height: 30em; overflow: auto;"}
     [:p (common/convert-md (:overview run))]]]])

(defpartial run-project
  [run]
  (let [projects (list-projects)]
    [:section#project
     [:div.page-header
      [:a {:name "project"}
       [:h2 "Project"]]]
     [:div.row
      [:div.span4.columns [:p "Choose an existing project, or create a new project."]]
      [:div.span12.columns
       (form-to
        [:post "/run/set-project"]
        (hidden-field :id (:_id run))
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
    [:div.span4.columns "&nbsp;"]
    [:div.span12.columns
     [:p "Delete run and all associated results?"]
     (form-to [:post "/run/delete-run"]
              (hidden-field :id (:_id run))
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
    (resp/redirect (format "/run/%s" (:id project)))
    (do
      (set-project (:id project) (if (= "New..." (:project-select project))
                                   (:new-project project) (:project-select project)))
      (resp/redirect (format "/run/%s#project" (:id project))))))

(defpage
  [:post "/run/delete-run"] {:as run}
  (common/layout
   "Confirm deletion"
   (common/confirm-deletion "/run/delete-run-confirm" (:id run)
                            "Are you sure you want to delete the run?")))

(defpage
  [:post "/run/delete-run-confirm"] {:as confirm}
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-run (:id confirm))
      (resp/redirect "/"))
    (resp/redirect (format "/run/%s" (:id confirm)))))

(defpage
  [:post "/run/set-fields"] {:as fields}
  (set-fields-funcs (:id fields) (dissoc fields :id :results-type) (:results-type fields))
  (resp/redirect (format "/run/%s#%s" (:id fields)
                         (format "%s-results" (name (:results-type fields))))))

(defpage "/run/:id" {id :id}
  (let [run (get-doc id)
        comparative? (= "comparative" (:paramstype run))
        comparative-fields (get-summary-fields run :comparative)
        control-fields (get-summary-fields run :control)]
    (common/layout
     (format "%s run %s" (:problem run) (subs id 22))
     [:div.row [:div.span16.columns
                [:h1 (format "%s run %s <small>(%s)</small>"
                             (:problem run) (subs id 22)
                             (:paramstype run))]]]
     (if comparative?
       (run-comparative-results-table run comparative-fields))
     (if comparative?
       (run-paired-results-table run control-fields))
     (if-not comparative?
       (run-non-comparative-results-table run control-fields))
     (analysis run)
     (graphs run)
     (annotations run "run")
     (run-claims run)
     (run-parameters run)
     (run-overview-notes run)
     (run-project run)
     (run-metainfo run)
     (run-delete-run run))))
