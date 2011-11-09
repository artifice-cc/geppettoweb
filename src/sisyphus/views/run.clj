(ns sisyphus.views.run
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:require [clojure.contrib.string :as str])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.runs :only
         [get-summary-results get-summary-fields set-summary-fields
          add-annotation delete-annotation
          set-graphs set-analysis delete-run]])
  (:use [sisyphus.models.graphs :only [list-graphs]])
  (:use [sisyphus.models.analysis :only [list-analysis]])
  (:use [sisyphus.models.claims :only [claim-select-options list-claims]])
  (:use [sisyphus.views.claims :only
         [claim-summary claim-association-form-non-comparative
          claim-association-form-comparative]])
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
    [:h2 "Metadata"]]
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
     [:div.page-header [:h2 "Parameters"]]
     (parameters-summary params)]))

(defpartial field-checkbox
  [n fieldstype field on-fields]
  [:li [:label
        [:input {:type "checkbox" :name (format "%s[]" (name n)) :value (name field)
                 :checked (on-fields (name field))}]
        " " (name field)]])

(defpartial field-checkboxes
  [run n fieldstype fields]
  (let [field-groups (partition-all (int (Math/ceil (/ (count fields) 3))) fields)
        on-fields (set (get run (keyword (format "%s-fields" (name fieldstype)))))]
    (map (fn [fs]
           [:div.span4.columns
            [:ul.inputs-list (map (fn [f] (field-checkbox n fieldstype f on-fields)) fs)]])
         field-groups)))

(defpartial run-fields-form
  [run fields fieldstype]
  (form-to
   [:post "/run/set-fields"]
   (hidden-field :id (:_id run))
   (hidden-field :fieldstype fieldstype)
   (hidden-field :problem (:problem run))
   [:div.row
    [:div.span4.columns
     [:p [:b [:a.fields_checkboxes_header "Select active fields..."]]]]]
   [:div.fields_checkboxes
    [:div.row
     [:div.span4.columns "&nbsp;"]
     (field-checkboxes run :fields fieldstype fields)]
    [:div.row
     [:div.span4.columns "&nbsp;"]
     [:div.span12.columns
      [:div.actions
       [:input.btn.primary {:value "Update" :type "submit"}]]]]]))

(defpartial run-comparative-results-table
  [run comparative-results comparative-fields]
  (let [on-fields (map keyword (:comparative-fields run))]
    [:section#comparative-results
     [:div.page-header
      [:a {:name "comparative-results"}]
      [:h2 "Comparative results"]]
     (results-table comparative-results on-fields)
     (run-fields-form run comparative-fields :comparative)]))

(defpartial run-paired-results-table
  [run control-results comparison-results paired-fields]
  (let [on-fields (map keyword (:paired-fields run))]
    [:section#paired-results
     [:div.page-header
      [:a {:name "control-comparison-results"}]
      [:h2 "Control/comparison results"]]
     (paired-results-table control-results comparison-results on-fields)
     (run-fields-form run paired-fields :paired)]))

(defpartial run-non-comparative-results-table
  [run results fields]
  (let [on-fields (map keyword (:non-comparative-fields run))]
    [:section#non-comparative-results
     [:div.page-header
      [:a {:name "results"}]
      [:h2 "Results"]]
     (results-table results on-fields)
     (run-fields-form run fields :non-comparative)]))

(defpartial run-claims-header
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

(defpartial run-claims-comparative
  [run comparative-fields paired-fields]
  (let [claim-opts (claim-select-options run)]
    [:div (run-claims-header run)
     (when (not-empty claim-opts)
       (claim-association-form-comparative nil run claim-opts comparative-fields paired-fields))]))

(defpartial run-claims-non-comparative
  [run fields]
  (let [claim-opts (claim-select-options run)]
    [:div (run-claims-header run)
     (when (not-empty claim-opts)
       (claim-association-form-non-comparative nil run claim-opts fields))]))

(defpartial run-overview-notes
  [run]
  [:section#overview
   [:div.page-header
    [:h2 "Overview notes"]]
   [:div.row
    [:div.span4.columns "&nbsp;"]
    [:div.span12.columns {:style "max-height: 30em; overflow: auto;"}
     [:p (common/convert-md (:overview run))]]]])

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
  [:post "/run/set-graphs"] {:as graphs}
  (set-graphs (:id graphs) (:graphs graphs))
  (resp/redirect (format "/run/%s#graphs" (:id graphs))))

(defpage
  [:post "/run/set-analysis"] {:as analysis}
  (set-analysis (:id analysis) (:analysis analysis))
  (resp/redirect (format "/run/%s#analysis" (:id analysis))))

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
  (set-summary-fields (:id fields) (:fieldstype fields) (:fields fields))
  (resp/redirect (format "/run/%s#%s" (:id fields)
                         (format "%s-results" (name (:fieldstype fields))))))

(defpage "/run/:id" {id :id}
  (let [run (get-doc id)
        comparative? (= "comparative" (:paramstype run))
        comparative-results (get-summary-results run :comparative)
        comparative-fields (get-summary-fields comparative-results)
        [control-results comparison-results]
        (map (fn [resultstype] (get-summary-results id resultstype))
             [:control :comparison])
        control-fields (get-summary-fields control-results)
        paired-fields (get-summary-fields (concat control-results comparison-results))]
    (common/layout
     (format "%s run %s" (:problem run) (subs id 22))
     [:div.row [:div.span16.columns
                [:h1 (format "%s run %s <small>(%s)</small>"
                             (:problem run) (subs id 22)
                             (:paramstype run))]]]
     (if comparative?
       (run-comparative-results-table run comparative-results comparative-fields))
     (if comparative?
       (run-paired-results-table run control-results comparison-results paired-fields))
     (if-not comparative?
       (run-non-comparative-results-table run control-results control-fields))
     (analysis run)
     (graphs run)
     (annotations run "run")
     (if comparative?
       (run-claims-comparative run comparative-fields paired-fields))
     (if-not comparative?
       (run-claims-non-comparative run control-fields))
     (run-parameters run)
     (run-overview-notes run)
     (run-metainfo run)
     (run-delete-run run))))
