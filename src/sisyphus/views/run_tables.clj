(ns sisyphus.views.run-tables
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.runs :only
         [get-summary-results get-summary-fields
          get-fields-funcs set-fields-funcs
          format-summary-fields]])
  (:use [sisyphus.views.fields :only [field-selects]])
  (:use [sisyphus.views.run :only
         [run-parameters run-overview-notes run-metainfo run-delete-run]])
  (:use [sisyphus.views.results :only
         [results-table paired-results-table]]))

(defpartial run-fields-form
  [run results-type fields fields-funcs]
  (form-to
   [:post "/run/tables/set-fields"]
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

(defpage
  [:post "/run/tables/set-fields"] {:as fields}
  (set-fields-funcs (:id fields) (dissoc fields :id :results-type) (:results-type fields))
  (resp/redirect (format "/run/tables/%s#%s" (:id fields)
                         (format "%s-results" (name (:results-type fields))))))

(defpage "/run/tables/:id" {id :id}
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
     (run-parameters run)
     (run-overview-notes run)
     (run-metainfo run)
     (run-delete-run run))))