(ns sisyphus.views.parameters
  (:require [clojure.contrib.string :as str])
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:require [noir.cookies :as cookies])
  (:require [clojure.set :as set])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [to-clj]])
  (:use [geppetto.parameters :only
         [parameters-latest? parameters-latest
          new-parameters update-parameters get-params
          list-parameters runs-with-parameters delete-parameters
          vectorize-params explode-params params-pairable?]])
  (:use [sisyphus.views.overview :only [runs-table]]))

(defpartial parameters-form
  [params]
  [:section#parameters-form
   [:div.page-header
    (if (and (:name params) (:problem params))
      [:h1 (format "Update %s/%s" (:problem params) (:name params))]
      [:a {:name "form"}
       [:h1 "New parameters"]])]
   [:form.form-horizontal {:method "POST" :action (if (:name params) "/parameters/update-parameters"
                                                      "/parameters/new-parameters")}
    (hidden-field :paramid (:paramid params))
    [:div.control-group
     [:label.control-label {:for "problem"} "Problem"]
     [:div.controls
      [:input.input-large {:id "problem" :name "problem" :size 30
                           :type "text" :value (:problem params)}]]]
    [:div.control-group
     [:label.control-label {:for "name"} "Name"]
     [:div.controls
      [:input.input-large {:id "name" :name "name" :size 30
                           :type "text" :value (:name params)}]]]
    [:div.control-group
     [:label.control-label {:for "description"} "Description"]
     [:div.controls
      [:textarea.input-xxlarge {:id "description" :name "description"}
       (:description params)]]]
    [:div.control-group
     [:label.control-label {:for "control"} "Control"]
     [:div.controls
      [:textarea.input-xxlarge {:id "control" :name "control" :rows 10}
       (:control params)]]]
    [:div.control-group
     [:label.control-label {:for "comparison"} "Comparison"]
     [:div.controls
      [:textarea.input-xxlarge {:id "comparison" :name "comparison" :rows 10}
       (:comparison params)]]]
    [:div.form-actions
     [:input.btn.btn-primary {:value (if (:name params) "Update" "Save")
                              :name "action" :type "submit"}]
     " "
     (if (and (:name params) (empty? (runs-with-parameters (:paramid params))))
       [:input.btn.btn-danger {:value "Delete" :name "action":type "submit"}])]]])

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
  (let [param-info (if (parameters-latest? (:paramid params))
                     [:p (link-to (format "/parameters/update/%s" (:paramid params)) "Update")]
                     [:p "This is an old version. "
                      (link-to (format "/parameters/%s" (:paramid (parameters-latest
                                                              (:problem params) (:name params))))
                               "View the latest version.")])]
    (if embedded?
      [:div.row-fluid
       [:div.span12.columns
        [:a {:name (format "params%d" (:paramid params))}
         [:h2 (format "%s/%s" (:problem params) (:name params))]]
        param-info
        [:p (:description params)]]]
      [:section
       [:div.page-header
        [:h1 (format "%s/%s" (:problem params) (:name params))]]
       param-info
       [:p (:description params)]]))
  (if (:comparison params)
    (let [control-params (to-clj (:control params))
          comparison-params (to-clj (:comparison params))]
      [:div
       (when-not (params-pairable? control-params comparison-params)
         [:div.row-fluid
          [:div.span12.columns
           [:p
            [:span.badge.badge-warning "Warning"]
            " These parameters cannot be paired, as they specify different keys."]]])
       [:div.row-fluid
        [:div.span6.columns
         [:h3 "Control"]
         [:div.params
          (paramscount control-params)
          (params-diff control-params comparison-params)]]
        [:div.span6.columns
         [:h3 "Comparison"]
         [:div.params
          (paramscount comparison-params)
          (params-diff comparison-params control-params)]]]])
    (let [control-params (to-clj (:control params))]
      [:div.row-fluid
       [:div.span6.columns
        [:div.params
         (paramscount control-params)
         [:pre (:control params)]]]]))
  [:h3 "Runs with these parameters"]
  (runs-table (runs-with-parameters (:paramid params)) (:problem params) false))

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
     [:section#comparative-parameters
      [:div.page-header
       [:a {:name "comparative"}
        [:h1 "Comparative parameters"]]]
      (for [params comparative]
        (parameters-summary params true))]
     [:section#non-comparative-parameters
      [:div.page-header
       [:a {:name "noncomparative"}
        [:h1 "Non-comparative parameters"]]]
      (for [params non-comparative]
        (parameters-summary params true))]
     (parameters-form nil))))
