(ns sisyphus.views.fields
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(defpartial field-checkbox
  [field on-fields]
  [:label.checkbox
   [:input {:type "checkbox" :name "fields[]" :value (name field)
            :checked (on-fields field)}] " " (name field)])

(defpartial field-checkboxes
  [on-fields fields]
  (let [field-groups (partition-all (int (Math/ceil (/ (count fields) 2))) fields)]
    (map (fn [fs]
         [:div.span6
          (map (fn [f] (field-checkbox f on-fields)) fs)])
       field-groups)))
