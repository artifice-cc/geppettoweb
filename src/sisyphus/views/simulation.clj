(ns sisyphus.views.simulation
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.simulations :only
         [get-simulation-fields set-simulation-fields]])
  (:use [sisyphus.models.graphs :only [list-graphs]])
  (:use [sisyphus.models.analysis :only [list-analysis]])
  (:use [sisyphus.views.graphs :only [graphs]])
  (:use [sisyphus.views.analysis :only [analysis]])
  (:use [sisyphus.views.annotations :only [annotations]])
  (:use [sisyphus.views.results :only
         [results-table paired-results-table]]))

(defpartial field-checkbox
  [field on-fields]
  [:li [:label [:input {:type "checkbox" :name "fields[]" :value (name field)
                        :checked (on-fields field)}] " " (name field)]])

(defpartial field-checkboxes
  [sim fieldstype fields on-fields]
  (let [field-groups (partition-all (int (Math/ceil (/ (count fields) 3))) fields)]
    (map (fn [fs]
           [:div.span4.columns
            [:ul.inputs-list (map (fn [f] (field-checkbox f on-fields)) fs)]])
         field-groups)))

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
  (let [on-fields (set (map keyword (:comparative-fields sim)))
        results (:comparative sim)]
    [:section#results
     [:div.page-header
      [:a {:name "comparative-results"}]
      [:h2 "Comparative results"]]
     (results-table results on-fields)
     (sim-fields-form sim :comparative on-fields)]))

(defpartial sim-paired-results-table
  [sim]
  (let [on-fields (set (map keyword (:control-fields sim)))
        control-results (:control sim)
        comparison-results (:comparison sim)
        paired-results (partition 2 (interleave control-results comparison-results))]
    [:section#paired-results
     [:div.page-header
      [:a {:name "control-comparison-results"}]
      [:h2 "Control/comparison results"]]
     (paired-results-table paired-results on-fields)
     (sim-fields-form sim :control on-fields)]))

(defpartial sim-non-comparative-results-table
  [sim]
  (let [on-fields (set (map keyword (:control-fields sim)))
        results (:control sim)]
    [:section#non-comparative-results
     [:div.page-header
      [:a {:name "results"}]
      [:h2 "Results"]]
     (results-table results on-fields)
     (sim-fields-form sim :control on-fields)]))

(defpartial sim-parameters
  [sim]
  (let [control-params (:control-params sim)
        comparison-params (:comparison-param sim)]
    (cond (= "comparative" (:type sim))
          [:div [:pre control-params] [:pre comparison-params]]
          (= "control" (:type sim))
          [:pre control-params]
          :else
          [:pre comparison-params])))

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
     (sim-parameters sim))))
