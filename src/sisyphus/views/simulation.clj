(ns sisyphus.views.simulation
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.simulations :only
         [get-simulation-fields set-simulation-fields]])
  (:use [sisyphus.models.annotations :only [add-annotation delete-annotation]])
  (:use [sisyphus.models.graphs :only [list-graphs]])
  (:use [sisyphus.models.analysis :only [list-analysis set-analysis]])
  (:use [sisyphus.views.fields :only [field-checkboxes]])
  (:use [sisyphus.views.graphs :only [graphs]])
  (:use [sisyphus.views.analysis :only [analysis]])
  (:use [sisyphus.views.annotations :only [annotations]])
  (:use [sisyphus.views.parameters :only [params-diff]])
  (:use [sisyphus.views.results :only
         [results-table paired-results-table]]))

(defpartial sim-fields-form
  [sim fieldstype on-fields]
  (let [fields (get-simulation-fields sim fieldstype)]
    (form-to
     [:post "/simulation/set-fields"]
     (hidden-field :id (:_id sim))
     (hidden-field :fieldstype fieldstype)
     [:div.row
      [:div.span4.columns
       [:p [:b [:a.fields_checkboxes_header "Select active fields..."]]]]]
     [:div.fields_checkboxes
      [:div.row
       [:div.span4.columns "&nbsp;"]
       (field-checkboxes sim fieldstype fields on-fields)]
      [:div.row
       [:div.span4.columns "&nbsp;"]
       [:div.span12.columns
        [:div.actions
         [:input.btn.primary {:value "Update" :type "submit"}]]]]])))

(defpartial sim-comparative-results-table
  [sim]
  (let [run (get-doc (:runid sim))
        on-fields (set (map keyword (:simulation-comparative-fields run)))
        results (:comparative sim)]
    [:section#results
     [:div.page-header
      [:a {:name "comparative-results"}]
      [:h2 "Comparative results"]]
     (results-table results on-fields)
     (sim-fields-form sim :comparative on-fields)]))

(defpartial sim-paired-results-table
  [sim]
  (let [run (get-doc (:runid sim))
        on-fields (set (map keyword (:simulation-control-fields run)))
        control-results (:control sim)
        comparison-results (:comparison sim)]
    [:section#paired-results
     [:div.page-header
      [:a {:name "control-comparison-results"}]
      [:h2 "Control/comparison results"]]
     (if (not= (count control-results) (count comparison-results))
       [:p "Cannot show paired results table, since control/comparison
            simulations had different number of decision points
            (either Steps or StepsBetween parameters differ)."]
       [:div
        (paired-results-table control-results comparison-results on-fields)
        (sim-fields-form sim :control on-fields)])]))

(defpartial sim-non-comparative-results-table
  [sim]
  (let [run (get-doc (:runid sim))
        on-fields (set (map keyword (:simulation-control-fields run)))
        results (:control sim)]
    [:section#non-comparative-results
     [:div.page-header
      [:a {:name "results"}]
      [:h2 "Results"]]
     (results-table results on-fields)
     (sim-fields-form sim :control on-fields)]))

(defpartial sim-comparative-parameters
  [sim]
  (let [control-params (read-string (:control-params (first (:control sim))))
        comparison-params (read-string (:comparison-params (first (:comparison sim))))]
    [:section#parameters
     [:div.page-header
      [:a {:name "parameters"}]
      [:h2 "Parameters"]]
     [:div.row
      [:div.span8.columns
       [:h3 "Control"]
       [:div.params
        (params-diff control-params comparison-params)]]
      [:div.span8.columns
       [:h3 "Comparison"]
       [:div.params
        (params-diff comparison-params control-params)]]]]))

(defpartial sim-parameters
  [sim]
  (let [params (:params (first (:control sim)))]
    [:section#parameters
     [:div.page-header
      [:a {:name "parameters"}]
      [:h2 "Parameters"]]
     [:div.row
      [:div.span8.columns
       [:div.params
        [:pre params]]]]]))

(defpage
  [:post "/simulation/delete-annotation"] {:as annotation}
  (delete-annotation (:id annotation) (Integer/parseInt (:index annotation)))
  (resp/redirect (format "/simulation/%s#annotations" (:id annotation))))

(defpage
  [:post "/simulation/add-annotation"] {:as annotation}
  (add-annotation (:id annotation) (:content annotation))
  (resp/redirect (format "/simulation/%s#annotations" (:id annotation))))

(defpage
  [:post "/simulation/set-fields"] {:as fields}
  (set-simulation-fields (:id fields) (:fieldstype fields) (:fields fields))
  (resp/redirect (format "/simulation/%s#%s" (:id fields)
                         (format "%s-results" (name (:fieldstype fields))))))

(defpage "/simulation/:id" {id :id}
  (let [sim (get-doc id)
        run (get-doc (:runid sim))
        comparative? (= "comparative" (:paramstype run))]
    (common/layout
     (format "%s simulation %s" (:problem run) (subs id 22))
     [:div.row [:div.span16.columns
                [:h1 (format "%s simluation %s"
                             (:problem run) (subs id 22) (:paramstype run)
                             (subs (:runid sim) 22))
                 [:br]
                 [:small "(" (:paramstype run) ", part of run "
                  (link-to (format "/run/%s" (:runid sim)) (subs (:runid sim) 22))
                  ")"]]]]
     (if comparative?
       (sim-comparative-results-table sim))
     (if comparative?
       (sim-paired-results-table sim))
     (if-not comparative?
       (sim-non-comparative-results-table sim))
     (analysis sim)
     (graphs sim)
     (annotations sim "simulation")
     (if comparative?
       (sim-comparative-parameters sim)
       (sim-parameters sim)))))
