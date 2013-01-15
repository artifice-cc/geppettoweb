(ns sisyphus.views.parameters
  (:require [clojure.contrib.string :as str])
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:require [noir.cookies :as cookies])
  (:require [clojure.set :as set])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [to-clj]])
  (:use [granary.parameters :only
         [parameters-latest? parameters-latest
          new-parameters update-parameters get-params
          list-parameters runs-with-parameters delete-parameters]])
  (:use [sisyphus.views.overview :only [runs-table]]))

(defpartial parameters-form
  [params]
  [:section#parameters-form
   [:div.page-header
    [:a {:name "form"}
     [:h1 (if (and (:name params) (:problem params))
            (format "Update parameters %s/%s" (:problem params) (:name params))
            "New parameters")]]]
   [:div.row
    [:div.span12.columns
     (form-to [:post (if (:name params) "/parameters/update-parameters"
                         "/parameters/new-parameters")]
              (hidden-field :paramid (:paramid params))
              [:fieldset
               [:div.clearfix
                [:label {:for "problem"} "Problem"]
                [:div.input
                 [:input.xlarge {:id "problem" :name "problem" :size 30
                                 :type "text" :value (:problem params)}]]]
               [:div.clearfix
                [:label {:for "name"} "Name"]
                [:div.input
                 [:input.xlarge {:id "name" :name "name" :size 30
                                 :type "text" :value (:name params)}]]]
               [:div.clearfix
                [:label {:for "description"} "Description"]
                [:div.input
                 [:textarea.xxlarge {:id "description" :name "description"}
                  (:description params)]]]]
              [:fieldset
               [:legend "Parameters"]
               [:div.clearfix
                [:label {:for "control"} "Control /<br/>Non-comparative"]
                [:div.input
                 [:textarea.xxlarge {:id "control" :name "control" :rows 10}
                  (:control params)]]]
               [:div.clearfix
                [:label {:for "comparison"} "Comparison<br/>(if comparative)"]
                [:div.input
                 [:textarea.xxlarge {:id "comparison" :name "comparison" :rows 10}
                  (:comparison params)]]]]
              [:div.actions
               [:input.btn.primary {:value (if (:name params) "Update" "Save")
                                    :name "action" :type "submit"}]
               " "
               (if (and (:name params) (empty? (runs-with-parameters (:paramid params))))
                 [:input.btn.danger {:value "Delete" :name "action":type "submit"}])])]]])

(defn vectorize-params
  [params]
  (try
    (reduce (fn [m k] (let [v (k params)]
                        (assoc m k (if (vector? v) v [v]))))
            {} (keys params))
    (catch Exception _ {})))

(defn explode-params
  "Want {:Xyz [1 2 3], :Abc [3 4]} to become [{:Xyz 1, :Abc 3}, {:Xyz 2, :Abc 4}, ...]"
  [params]
  (when (not-empty params)
    (if (= 1 (count params))
      (for [v (second (first params))]
        {(first (first params)) v})
      (let [p (first params)
            deeper (explode-params (rest params))]
        (flatten (map (fn [v] (map #(assoc % (first p) v) deeper)) (second p)))))))

(defpartial params-diff
  [ps1 ps2]
  (try
    (let [common-keys (set/intersection (set (keys ps1)) (set (keys ps2)))
          unique-keys (set/difference (set (keys ps1)) (set (keys ps2)))]
      [:pre
       "{\n"
       (for [k (sort common-keys)]
         (if (= (ps1 k) (ps2 k))
           (format "%s %s\n" k (pr-str (ps1 k)))
           [:b (format "%s %s\n" k (pr-str (ps1 k)))]))
       (for [k (sort unique-keys)]
         [:b (format "%s %s\n" k (pr-str (ps1 k)))])
       "}"])
    (catch Exception _)))

(defpartial paramscount
  [params]
  [:span.paramscount (count (explode-params (vectorize-params params)))
   [:br [:small "params"]]])

(defpartial parameters-summary
  [params embedded?]
  [:div.row
   [:div.span12.columns
    [:a {:name (format "params%d" (:paramid params))}
     [(if embedded? :h3 :h2) (format "%s/%s" (:problem params) (:name params))]]
    (if (parameters-latest? (:paramid params))
      [:p (link-to (format "/parameters/update/%s" (:paramid params)) "Update")]
      [:p "This is an old version. "
       (link-to (format "/parameters#params%s" (:paramid (parameters-latest
                                                     (:problem params) (:name params))))
                "View the latest version.")])
    [:p (:description params)]]]
  (if (:comparison params)
    (let [control-params (to-clj (:control params))
          comparison-params (to-clj (:comparison params))]
      [:div.row
       [:div.span6.columns
        [:h3 "Control"]
        [:div.params
         (paramscount control-params)
         (params-diff control-params comparison-params)]]
       [:div.span6.columns
        [:h3 "Comparison"]
        [:div.params
         (paramscount comparison-params)
         (params-diff comparison-params control-params)]]])
    (let [control-params (to-clj (:control params))]
      [:div.row
       [:div.span6.columns
        [:div.params
         (paramscount control-params)
         [:pre (:control params)]]]]))
  [:div
   [:h3 "Runs with these parameters"]]
  (runs-table (runs-with-parameters (:paramid params)) (:problem params)))

(defpage
  [:post "/parameters/update-parameters"] {:as params}
  (cond (= "Update" (:action params))
        (do
          (let [paramid-new (update-parameters params)]
            (resp/redirect (format "/parameters#params%s" paramid-new))))
        (= "Delete" (:action params))
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/parameters/delete-parameters-confirm" (:paramid params)
                                  "Are you sure you want to delete the parameters?"))
        :else
        (resp/redirect (format "/parameters#params%s" (:paramid params)))))

(defpage
  [:post "/parameters/delete-parameters-confirm"] {:as confirm}
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-parameters (:id confirm))
      (resp/redirect "/parameters"))
    (resp/redirect (format "/parameters#params%s" (:id confirm)))))

(defpage
  [:post "/parameters/new-parameters"] {:as params}
  (let [paramid-new (new-parameters params)]
    (resp/redirect (format "/parameters#params%s" paramid-new))))

(defpage "/parameters/update/:paramid" {paramid :paramid}
  (if-let [params (get-params paramid)]
    (common/layout
     (format "Parameters: %s/%s" (:problem params) (:name params))
     (parameters-form params))
    (resp/redirect "/parameters")))

(defpage "/parameters/:paramid" {paramid :paramid}
  (if-let [params (get-params paramid)]
    (common/layout
     (format "Parameters: %s/%s" (:problem params) (:name params))
     (parameters-summary params false))
    (resp/redirect "/parameters")))

(defpage "/parameters" {}
  (let [{:keys [comparative non-comparative]} (list-parameters)]
    (common/layout
     "Parameters"
     [:div
      [:section#comparative-parameters
       [:div.page-header
        [:a {:name "comparative"}
         [:h1 "Comparative parameters"]]]
       (for [params comparative]
         (parameters-summary params false))]
      [:section#non-comparative-parameters
       [:div.page-header
        [:a {:name "noncomparative"}
         [:h1 "Non-comparative parameters"]]]
       (for [params non-comparative]
         (parameters-summary params false))]
      (parameters-form nil)])))
