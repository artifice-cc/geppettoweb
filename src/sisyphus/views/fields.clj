(ns sisyphus.views.fields
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(defpartial field-select
  [field fields-funcs]
  [:li
   [:label
    [:select.mini {:name field}
     (for [func ["N/A" "min" "max" "avg" "sum" "first" "last"]]
       [:option {:value func
                 :selected (some #(= [field func] %) fields-funcs)}
        func])]
    " " (name field)]])

(defpartial field-selects
  [run results-type fields fields-funcs]
  (let [field-groups (partition-all (int (Math/ceil (/ (count fields) 3))) fields)]
    (map (fn [fs]
           [:div.span4.columns
            [:ul.inputs-list (map (fn [f] (field-select f fields-funcs)) fs)]])
         field-groups)))

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
