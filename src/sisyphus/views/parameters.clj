(ns sisyphus.views.parameters
  (:require [clojure.contrib.string :as str])
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:require [noir.cookies :as cookies])
  (:require [clojure.set :as set])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.runs :only [problem-fields]])
  (:use [sisyphus.models.parameters :only
         [new-parameters update-parameters list-parameters runs-with-parameters delete-parameters]])
  (:use [sisyphus.views.overview :only [runs-table]]))

(defpartial parameters-form
  [params]
  [:section#parameters-form
   [:div.page-header
    [:h1 (if (and (:name params) (:problem params))
           (format "Update parameters %s/%s" (:problem params) (:name params))
           "New parameters")]]
   [:div.row
    [:div.span4.columns "&nbsp;"]
    [:div.span12.columns
     (form-to [:post (if (:name params) "/parameters/update-parameters"
                         "/parameters/new-parameters")]
              (hidden-field :id (:_id params))
              [:fieldset
               [:legend "Metadata"]
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
                  (:description params)]]]
               [:div.clearfix
                [:div.input
                 (radio-button "paramstype" (= "comparative" (:paramstype params))
                               "comparative") " Comparative"
                 " "
                 (radio-button "paramstype" (= "non-comparative" (:paramstype params))
                               "non-comparative") " Non-comparative"]]]
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
               (if (and (:name params) (empty? (runs-with-parameters params)))
                 [:input.btn.danger {:value "Delete" :name "action":type "submit"}])])]]])

(defpartial params-diff
  [params1 params2]
  (let [ps1 (read-string params1)
        ps2 (read-string params2)
        common-keys (set/intersection (set (keys ps1)) (set (keys ps2)))
        unique-keys (set/difference (set (keys ps1)) (set (keys ps2)))]
    [:pre
     "{\n"
     (for [k (sort common-keys)]
       (if (= (ps1 k) (ps2 k))
         (format "%s %s\n" k (pr-str (ps1 k)))
         [:b (format "%s %s\n" k (pr-str (ps1 k)))]))
     (for [k (sort unique-keys)]
       [:b (format "%s %s\n" k (pr-str (ps1 k)))])
     "}"]))

(defpartial parameters-summary
  [params]
  [:div.row
   [:div.span16.columns
    [:a {:name (:_id params)}]
    [:h2 (format "%s/%s" (:problem params) (:name params))]
    (if (or (nil? (:revs params))
            (= (:start (:revs params)) (Integer/parseInt (first (str/split #"-" (:_rev params))))))
      [:p (link-to (format "/parameters/%s" (:_id params)) "Update")]
      [:p "This is an old version. "
       (link-to (format "/parameters/%s" (:_id params)) "View the latest version.")])
    [:p (:description params)]]]
  (if (= "comparative" (:paramstype params))
    [:div.row
     [:div.span8.columns
      [:h3 "Control"]
      (params-diff (:control params) (:comparison params))]
     [:div.span8.columns
      [:h3 "Comparison"]
      (params-diff (:comparison params) (:control params))]]
    [:div.row
     [:div.span8.columns
      [:pre (:control params)]]])
  [:div
   [:h3 "Runs with these parameters"]]
  (let [fields (problem-fields (:problem params))
        custom-field (or (cookies/get (keyword (format "%s-field" (:problem params)))) (first fields))
        custom-func (or (cookies/get (keyword (format "%s-func" (:problem params)))) "SUM")]
    (runs-table (runs-with-parameters params) (:problem params)
                {:field custom-field :func custom-func})))

(defpage
  [:post "/parameters/update-parameters"] {:as params}
  (cond (= "Update" (:action params))
        (do
          (update-parameters params)
          (resp/redirect (format "/parameters#%s" (:id params))))
        (= "Delete" (:action params))
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/parameters/delete-parameters-confirm" (:id params)
                                  "Are you sure you want to delete the parameters?"))
        :else
        (resp/redirect (format "/parameters#%s" (:id params)))))

(defpage
  [:post "/parameters/delete-parameters-confirm"] {:as confirm}
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-parameters (:id confirm))
      (resp/redirect "/parameters"))
    (resp/redirect (format "/parameters#%s" (:id confirm)))))

(defpage
  [:post "/parameters/new-parameters"] {:as params}
  (let [id (:_id (new-parameters params))]
    (resp/redirect (format "/parameters#%s" id))))

(defpage "/parameters/:id" {id :id}
  (if-let [params (get-doc id)]
    (common/layout
     (format "Parameters: %s/%s" (:problem params) (:name params))
     (parameters-form params))
    (resp/redirect "/parameters")))

(defpage "/parameters/:id/:rev" {id :id rev :rev}
  (if-let [params (get-doc id rev)]
    (common/layout
     (format "Parameters: %s/%s" (:problem params) (:name params))
     (parameters-summary params))
    (resp/redirect "/parameters")))

(defpage "/parameters" {}
  (let [{:keys [comparative non-comparative]} (list-parameters)]
    (common/layout
     "Parameters"
     [:div
      [:section#comparative-parameters
       [:div.page-header [:h1 "Comparative parameters"]]
       (for [params comparative]
         (parameters-summary params))]
      [:section#non-comparative-parameters
       [:div.page-header [:h1 "Non-comparative parameters"]]
       (for [params non-comparative]
         (parameters-summary params))]
      (parameters-form nil)])))
