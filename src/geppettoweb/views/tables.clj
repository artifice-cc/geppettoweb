(ns geppettoweb.views.tables
  (:use [geppettoweb.views.common :only [gurl]])
  (:require [geppettoweb.views.common :as common])
  (:require [ring.util.response :as resp])
  (:use compojure.core hiccup.def hiccup.element hiccup.form hiccup.util)
  (:use [geppetto.runs :only
         [get-run gather-results-fields get-results]])
  (:use [geppettoweb.models.tables :only [get-table-fields set-table-fields]])
  (:use [geppettoweb.views.fields :only [field-checkboxes]])
  (:use [geppettoweb.views.run :only
         [run-parameters run-metainfo run-delete-run]])
  (:use [geppettoweb.views.results :only
         [results-table paired-results-table]]))

(defhtml run-fields-form
  [runid tabletype on-fields fields]
  [:form {:action (gurl "/run/tables/set-fields") :method "POST"}
   (hidden-field :runid runid)
   (hidden-field :tabletype tabletype)
   [:div.row-fluid
    [:div.span12.columns
     [:p [:b [:a.fields_checkboxes_header "Select active fields..."]]]]]
   [:div.fields_checkboxes
    [:div.row-fluid
     (field-checkboxes on-fields fields)]
    [:div.row-fluid
     [:div.span12.columns
      [:div.form-actions
       [:input.btn.btn-primary {:value "Update" :type "submit"}]]]]]])

(defhtml run-comparative-results-table
  [runid]
  (let [comparative-fields (gather-results-fields runid :comparative)
        on-fields (get-table-fields runid :comparative)
        results (get-results runid :comparative on-fields)]
    [:section#comparative-results
     [:a {:name "comparative-results"}
      [:h2 "Comparative results"]]
     (results-table results on-fields)
     (run-fields-form runid :comparative on-fields comparative-fields)]))

(defhtml run-paired-results-table
  [runid]
  (let [control-fields (gather-results-fields runid :control)
        on-fields (get-table-fields runid :paired)
        control-results (get-results runid :control on-fields)
        comparison-results (get-results runid :comparison on-fields)]    
    [:section#paired-results
     [:a {:name "control-comparison-results"}
      [:h2 "Control/comparison results"]]
     (paired-results-table control-results comparison-results on-fields)
     (run-fields-form runid :paired on-fields control-fields)]))

(defhtml run-non-comparative-results-table
  [runid]
  (let [control-fields (gather-results-fields runid :control)
        on-fields (get-table-fields runid :non-comparative)
        results (get-results runid :control on-fields)]
    (prn results)
    [:section#non-comparative-results
     [:a {:name "results"}
      [:h1 "Results"]]
     (results-table results on-fields)
     (run-fields-form runid :non-comparative on-fields control-fields)]))

(defn set-fields
  [runid tabletype fields]
  (set-table-fields runid tabletype fields)
  (resp/redirect (gurl (format "/run/tables/%s#%s" runid (format "%s-results" (name tabletype))))))

(defn show-tables
  [runid]
  (let [run (get-run runid)]
    (common/layout
     (format "%s/%s run %s" (:problem run) (:name run) runid)
     [:div.header.jumbotron.subhead
      [:div.row-fluid
       [:h1 (format "%s/%s run %s <small>(%s)</small>"
               (:problem run) (:name run)
               (link-to (gurl (format "/run/%s" runid)) runid)
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

(defroutes tables-routes
  (context "/run/tables" []
    (POST "/set-fields" [runid tabletype fields]
      (set-fields runid tabletype fields))
    (GET "/:runid" [runid]
      (show-tables runid))))
