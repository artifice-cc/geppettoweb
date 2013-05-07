(ns sisyphus.views.tables
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [geppetto.runs :only
         [get-run gather-results-fields get-results]])
  (:use [sisyphus.models.tables :only [get-table-fields set-table-fields]])
  (:use [sisyphus.views.fields :only [field-checkboxes]])
  (:use [sisyphus.views.run :only
         [run-parameters run-metainfo run-delete-run]])
  (:use [sisyphus.views.results :only
         [results-table paired-results-table]]))

(defpartial run-fields-form
  [runid tabletype on-fields fields]
  (form-to
   [:post "/run/tables/set-fields"]
   (hidden-field :runid runid)
   (hidden-field :tabletype tabletype)
   [:div.row
    [:div.span12.columns
     [:p [:b [:a.fields_checkboxes_header "Select active fields..."]]]]]
   [:div.fields_checkboxes
    [:div.row
     (field-checkboxes on-fields fields)]
    [:div.row
     [:div.span12.columns
      [:div.actions
       [:input.btn.primary {:value "Update" :type "submit"}]]]]]))

(defpartial run-comparative-results-table
  [runid]
  (let [comparative-fields (gather-results-fields runid :comparative)
        on-fields (get-table-fields runid :comparative)
        results (get-results runid :comparative on-fields)]
    [:section#comparative-results
     [:div.page-header
      [:a {:name "comparative-results"}
       [:h2 "Comparative results"]]]
     (results-table results on-fields)
     (run-fields-form runid :comparative on-fields comparative-fields)]))

(defpartial run-paired-results-table
  [runid]
  (let [control-fields (gather-results-fields runid :control)
        on-fields (get-table-fields runid :paired)
        control-results (get-results runid :control on-fields)
        comparison-results (get-results runid :comparison on-fields)]    
    [:section#paired-results
     [:div.page-header
      [:a {:name "control-comparison-results"}
       [:h2 "Control/comparison results"]]]
     (paired-results-table control-results comparison-results on-fields)
     (run-fields-form runid :paired on-fields control-fields)]))

(defpartial run-non-comparative-results-table
  [runid]
  (let [control-fields (gather-results-fields runid :control)
        on-fields (get-table-fields runid :non-comparative)
        results (get-results runid :control on-fields)]
    [:section#non-comparative-results
     [:div.page-header
      [:a {:name "results"}
       [:h2 "Results"]]]
     (results-table results on-fields)
     (run-fields-form runid :non-comparative on-fields control-fields)]))

(defpage
  [:post "/run/tables/set-fields"] {:as fields}
  (set-table-fields (:runid fields) (:tabletype fields) (:fields fields))
  (resp/redirect (format "/run/tables/%s#%s" (:runid fields)
                         (format "%s-results" (name (:tabletype fields))))))

(defpage "/run/tables/:runid" {runid :runid}
  (let [run (get-run runid)]
    (common/layout
     (format "%s/%s run %s" (:problem run) (:name run) runid)
     [:div.row [:div.span12.columns
                [:h1 (format "%s/%s run %s <small>(%s)</small>"
                        (:problem run) (:name run)
                        (format "<a href=\"/run/%s\">%s</a>" runid runid)
                        (if (:comparison run)
                          "comparative" "non-comparative"))]]]
     (if (:comparison run)
       (run-comparative-results-table runid))
     (if (:comparison run)
       (run-paired-results-table runid))
     (if (nil? (:comparison run))
       (run-non-comparative-results-table runid))
     (run-parameters run)
     (run-metainfo run)
     (run-delete-run run))))
