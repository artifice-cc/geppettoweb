(ns sisyphus.views.details
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:require [clojure.contrib.string :as str])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.runs :only
         [get-results get-fields set-fields add-annotation delete-annotation delete-run]])
  (:use [sisyphus.models.graphs :only [get-graph-png list-graphs]])
  (:use [sisyphus.models.analysis :only [list-analysis]])
  (:use [sisyphus.models.claims :only [claim-select-options list-claims]])
  (:use [sisyphus.views.claims :only
         [claim-summary claim-association-form-non-comparative
          claim-association-form-comparative]])
  (:use [sisyphus.views.graphs :only [show-graph]])
  (:use [sisyphus.views.analysis :only [show-analysis]])
  (:use [sisyphus.views.parameters :only [parameters-summary]])
  (:use [sisyphus.views.results :only
         [field-checkboxes results-table paired-results-table]]))

(defpartial details-metainfo
  [run]
  [:section#metadata
   [:div.page-header
    [:h2 "Metadata"]]
   [:div.row
    [:div.span4.columns
     [:h3 "Commit message"]
     [:p (link-to (format "https://bitbucket.org/joshuaeckroth/retrospect/changeset/%s" (:commit run))
                  (subs (:commit run) 0 10))
      " @ " (:branch run)]]
    [:div.span12.columns
     [:pre (:commit-msg run)]]]
   [:div.row
    [:div.span4.columns
     [:h3 "Simulation properties"]]
    [:div.span4.columns
     [:dl [:dt "User@hostname"]
      [:dd (format "%s@%s" (:username run) (:hostname run))]]
     [:dl [:dt "Time"]
      [:dd (common/date-format (:time run))]]
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
      [:dd (:recorddir run)]]]]])

(defpartial details-parameters
  [run]
  (let [params (get-doc (:paramsid run) (:paramsrev run))]
    [:section#parameters
     [:div.page-header [:h2 "Parameters"]]
     (parameters-summary params)]))

(defpartial details-fields-form
  [run fields fieldstype]
  (form-to
   [:post "/details/set-fields"]
   (hidden-field :id (:_id run))
   (hidden-field :fieldstype fieldstype)
   (hidden-field :problem (:problem run))
   [:div.row
    [:div.span4.columns [:h3.fields_checkboxes_header "Select active fields..."]]]
   [:div.fields_checkboxes
    [:div.row
     [:div.span4.columns "&nbsp;"]
     (field-checkboxes run :fields fieldstype fields)]
    [:div.row
     [:div.span4.columns "&nbsp;"]
     [:div.span12.columns
      [:div.actions
       [:input.btn.primary {:value "Update" :type "submit"}]]]]]))

(defpartial details-comparative-results-table
  [run comparative-results comparative-fields]
  (let [on-fields (map keyword (:comparative-fields run))]
    [:section#comparative-results
     [:div.page-header
      [:a {:name "comparative-results"}]
      [:h2 "Comparative results"]]
     (results-table comparative-results on-fields)
     (details-fields-form run comparative-fields :comparative)]))

(defpartial details-paired-results-table
  [run control-results comparison-results paired-fields]
  (let [on-fields (map keyword (:paired-fields run))]
    [:section#paired-results
     [:div.page-header
      [:a {:name "control-comparison-results"}]
      [:h2 "Control/comparison results"]]
     (paired-results-table control-results comparison-results on-fields)
     (details-fields-form run paired-fields :paired)]))

(defpartial details-non-comparative-results-table
  [run results fields]
  (let [on-fields (map keyword (:non-comparative-fields run))]
    [:section#non-comparative-results
     [:div.page-header
      [:a {:name "results"}]
      [:h2 "Results"]]
     (results-table results on-fields)
     (details-fields-form run fields :non-comparative)]))

(defpartial details-annotations
  [run]
  [:section#annotations
   [:div.page-header
    [:a {:name "annotations"}
     [:h2 "Annotations"]]]]
  [:div.row
   [:div.span4.columns "&nbsp;"]
   [:div.span8.columns
    (if (or (nil? (:annotations run)) (empty? (:annotations run)))
      [:p "No annotations."]
      (map (fn [i]
             (form-to [:post "/details/delete-annotation"]
                      (hidden-field :id (:_id run))
                      (hidden-field :index i)
                      [:blockquote [:p (nth (:annotations run) i)]]
                      [:p {:style "text-align: right;"} (submit-button "Delete")]))
           (range (count (:annotations run)))))]]
  [:div.row
   [:div.span4.columns
    [:h3 "New annotation"]]
   [:div.span12.columns
    (form-to
     [:post "/details/add-annotation"]
     (hidden-field :id (:_id run))
     [:div.clearfix
      [:label {:for "content"} "Content"]
      [:div.input
       [:textarea.xxlarge {:id "content" :name "content"}]]]
     [:div.actions
      [:input.btn.primary {:value "Save" :type "submit"}]])]])

(defpartial details-graphs
  [run]
  (let [graphs (filter #(= (:paramstype run) (:resultstype %))
                       (get (list-graphs) (:problem run)))]
    [:section#graphs
     [:div.page-header
      [:h2 "Graphs"]]
     (if (empty? graphs)
       [:div.row
        [:div.span16.columns [:p "No graphs."]]]
       (for [g graphs]
         (show-graph run g)))]))

(defpartial details-analysis
  [run]
  (let [analysis (filter #(= (:paramstype run) (:resultstype %))
                         (get (list-analysis) (:problem run)))]
    [:section#analysis
     [:div.page-header
      [:h2 "Analysis"]]
     (if (empty? analysis)
       [:div.row
        [:div.span16.columns [:p "No analysis."]]]
       (for [a analysis]
         (show-analysis run a)))]))

(defpartial details-claims-header
  [run]
  (let [run-claims (list-claims run)]
    [:section#claims
     [:div.page-header
      [:a {:name "claims"}
       [:h2 "Claims"]]]
     [:div.row
      [:div.span4.columns
       [:h2 "Associated claims"]]
      [:div.span12.columns
       (if (and (empty? (:verified run-claims))
                (empty? (:unverified run-claims)))
         [:p "No claims."]
         [:div
          (if (not-empty (:unverified run-claims))
            [:h3 "Unverified"])
          (for [c (:unverified run-claims)]
            (claim-summary c))
          (if (not-empty (:verified run-claims))
            [:h3 "Verified"])
          (for [c (:verified run-claims)]
            (claim-summary c))])]]]))

(defpartial details-claims-comparative
  [run comparative-fields paired-fields]
  (let [claim-opts (claim-select-options run)]
    [:div (details-claims-header run)
     (when (not-empty claim-opts)
       (claim-association-form-comparative nil run claim-opts comparative-fields paired-fields))]))

(defpartial details-claims-non-comparative
  [run fields]
  (let [claim-opts (claim-select-options run)]
    [:div (details-claims-header run)
     (when (not-empty claim-opts)
       (claim-association-form-non-comparative nil run claim-opts fields))]))

(defpartial details-overview-notes
  [run]
  [:section#overview
   [:div.page-header
    [:h2 "Overview notes"]]
   [:div.row
    [:div.span4.columns "&nbsp;"]
    [:div.span12.columns {:style "max-height: 30em; overflow: auto;"}
     [:p (common/convert-md (:overview run))]]]])

(defpartial details-delete-run
  [run]
  [:section#delete
   [:div.page-header
    [:h2 "Delete"]]
   [:div.row
    [:div.span4.columns "&nbsp;"]
    [:div.span12.columns
     [:p "Delete run and all associated results?"]
     (form-to [:post "/details/delete-run"]
              (hidden-field :id (:_id run))
              [:div.actions
               [:input.btn.danger {:value "Delete run" :type "submit"}]])]]])

(defpage
  [:post "/details/delete-annotation"] {:as annotation}
  (delete-annotation (:id annotation) (Integer/parseInt (:index annotation)))
  (resp/redirect (format "/details/%s#annotations" (:id annotation))))

(defpage
  [:post "/details/add-annotation"] {:as annotation}
  (add-annotation (:id annotation) (:content annotation))
  (resp/redirect (format "/details/%s#annotations" (:id annotation))))

(defpage
  [:post "/details/delete-run"] {:as run}
  (common/layout
   "Confirm deletion"
   (common/confirm-deletion "/details/delete-run-confirm" (:id run)
                            "Are you sure you want to delete the run?")))

(defpage
  [:post "/details/delete-run-confirm"] {:as confirm}
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-run (:id confirm))
      (resp/redirect "/"))
    (resp/redirect (format "/details/%s" (:id confirm)))))

(defpage
  [:post "/details/set-fields"] {:as fields}
  (set-fields (:id fields) (:fieldstype fields) (:fields fields))
  (resp/redirect (format "/details/%s#%s" (:id fields)
                         (format "%s-results" (name (:fieldstype fields))))))

(defpage "/details/:id" {id :id}
  (let [doc (get-doc id)]
    (if (= "run" (:type doc))
      (let [comparative? (= "comparative" (:paramstype doc))
            comparative-results (get-results (:_id doc) :comparative)
            comparative-fields (get-fields comparative-results)
            [control-results comparison-results]
            (map (fn [resultstype] (get-results (:_id doc) resultstype))
                 [:control :comparison])
            control-fields (get-fields control-results)
            paired-fields (get-fields (concat control-results comparison-results))]
        (common/layout
         (format "%s run %s" (:problem doc) (subs id 22))
         [:div.row [:div.span16.columns
                    [:h1 (format "%s run %s <small>(%s)</small>"
                                 (:problem doc) (subs id 22)
                                 (common/date-format (:time doc)))]]]
         (if comparative?
           (details-comparative-results-table doc comparative-results comparative-fields))
         (if comparative?
           (details-paired-results-table doc control-results comparison-results paired-fields))
         (if-not comparative?
           (details-non-comparative-results-table doc control-results control-fields))
         (details-analysis doc)
         (details-graphs doc)
         (details-annotations doc)
         (if comparative?
           (details-claims-comparative doc comparative-fields paired-fields))
         (if-not comparative?
           (details-claims-non-comparative doc control-fields))
         (details-parameters doc)
         (details-overview-notes doc)
         (details-metainfo doc)
         (details-delete-run doc)))
      (common/layout
       "Not supported"
       [:h1 "Not supported"]
       [:p "Details pages for information other than runs are not yet supported."]))))
