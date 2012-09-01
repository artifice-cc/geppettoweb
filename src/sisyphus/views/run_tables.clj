(ns sisyphus.views.run-tables
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.runs :only
         [get-summary-results get-summary-fields get-fields set-fields]])
  (:use [sisyphus.views.fields :only [field-checkboxes]])
  (:use [sisyphus.views.run :only
         [run-parameters run-overview-notes run-metainfo run-delete-run]])
  (:use [sisyphus.views.results :only
         [results-table paired-results-table]]))

(defpartial run-fields-form
  [run results-type on-fields fields]
  (form-to
   [:post "/run/tables/set-fields"]
   (hidden-field :id (:_id run))
   (hidden-field :results-type results-type)
   [:div.row
    [:div.span12.columns
     [:p [:b [:a.fields_checkboxes_header "Select active fields..."]]]]]
   [:div.fields_checkboxes
    [:div.row
     [:div.span12.columns "&nbsp;"]
     (field-checkboxes on-fields fields)]
    [:div.row
     [:div.span12.columns
      [:div.actions
       [:input.btn.primary {:value "Update" :type "submit"}]]]]]))

(defpartial run-comparative-results-table
  [run comparative-fields]
  (let [on-fields (get-fields run :comparative)
        results (get-summary-results run :comparative on-fields)]
    [:section#comparative-results
     [:div.page-header
      [:a {:name "comparative-results"}
       [:h2 "Comparative results"]]]
     (results-table results on-fields)
     (run-fields-form run :comparative on-fields comparative-fields)]))

(defpartial run-paired-results-table
  [run control-fields]
  (let [on-fields (get-fields run :paired)
        control-results (get-summary-results run :control on-fields)
        comparison-results (get-summary-results run :comparison on-fields)]
    [:section#paired-results
     [:div.page-header
      [:a {:name "control-comparison-results"}
       [:h2 "Control/comparison results"]]]
     (paired-results-table control-results comparison-results on-fields)
     (run-fields-form run :paired on-fields control-fields)]))

(defpartial run-non-comparative-results-table
  [run control-fields]
  (let [on-fields (get-fields run :non-comparative)
        results (get-summary-results run :control on-fields)]
    [:section#non-comparative-results
     [:div.page-header
      [:a {:name "results"}
       [:h2 "Results"]]]
     (results-table results on-fields)
     (run-fields-form run :non-comparative on-fields control-fields)]))

(defpage
  [:post "/run/tables/set-fields"] {:as fields}
  (set-fields (:id fields) (:fields fields) (:results-type fields))
  (resp/redirect (format "/run/tables/%s#%s" (:id fields)
                         (format "%s-results" (name (:results-type fields))))))

(defpage "/run/tables/:id" {id :id}
  (let [run (get-doc id)
        comparative? (= "comparative" (:paramstype run))
        comparative-fields (get-summary-fields run :comparative)
        control-fields (get-summary-fields run :control)]
    (common/layout
     (format "%s run %s" (:problem run) (subs id 22))
     [:div.row [:div.span12.columns
                [:h1 (format "%s run %s <small>(%s)</small>"
                        (:problem run) (format "<a href=\"/run/%s\">%s</a>" id (subs id 22))
                        (:paramstype run))]]]
     (if comparative?
       (run-comparative-results-table run comparative-fields))
     (if comparative?
       (run-paired-results-table run control-fields))
     (if-not comparative?
       (run-non-comparative-results-table run control-fields))
     (run-parameters run)
     #_(run-overview-notes run)
     (run-metainfo run)
     (run-delete-run run))))