(ns sisyphus.views.analysis
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:require [clojure.string :as str])
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.analysis :only
         [list-analysis new-analysis update-analysis
          set-analysis get-analysis-output delete-analysis]])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(defpartial show-analysis
  [doc analysis]
  [:div
   [:div.row
    [:div.span12.columns
     [:h3 (:name analysis) [:br]
      [:small (format " (%s, %s)" (:run-or-sim analysis) (:resultstype analysis))]]
     [:p (:caption analysis)]]]
   [:div.row
    [:div.span12.columns
     [:pre (get-analysis-output doc analysis)]]]])

(defpartial analysis-form
  [analysis]
  [:section#analysis-form
   [:div.page-header
    [:a {:name "form"}
     [:h1 (if (:name analysis) (format "Update analysis %s" (:name analysis))
              "New analysis")]]]
   [:div.row
    [:div.span4.columns
     [:h2 "Metadata"]]
    [:div.span12.columns
     (form-to [:post (if (:name analysis) "/analysis/update-analysis"
                         "/analysis/new-analysis")]
              (hidden-field :id (:_id analysis))
              [:fieldset
               [:legend "Metadata"]
               [:div.clearfix
                [:label {:for "problem"} "Problem"]
                [:div.input
                 [:input.xlarge {:id "problem" :name "problem" :size 30
                                 :type "text" :value (:problem analysis)}]]]
               [:div.clearfix
                [:label {:for "name"} "Name"]
                [:div.input
                 [:input.xlarge {:id "name" :name "name" :size 30
                                 :type "text" :value (:name analysis)}]]]
               [:div.clearfix
                [:label {:for "run-or-sim"} "Run or simulation?"]
                [:div.input
                 (drop-down :run-or-sim ["run" "simulation"]
                            (:run-or-sim analysis))]]
               [:div.clearfix
                [:label {:for "resultstype"} "Results type"]
                [:div.input
                 (drop-down :resultstype ["non-comparative" "comparative"]
                            (:resultstype analysis))]]
               [:div.clearfix
                [:label {:for "caption"} "Caption"]
                [:div.input
                 [:textarea.xxlarge {:id "caption" :name "caption"} (:caption analysis)]]]
               [:div.clearfix
                [:label {:for "code"} "R code"]
                [:div.input
                 [:textarea.xxlarge {:id "code" :name "code" :rows 10
                                     :style "font-family: monospace;"}
                  (if (:code analysis) (:code analysis)
                      "xyz")]
                 [:span.help-block "Assume the existence of data tables named 'control',
                                    'comparison', and 'comparative'."]]]
               [:div.actions
                [:input.btn.primary
                 {:name "action" :value (if (:name analysis) "Update" "Save")
                  :type "submit"}]
                " "
                (if (:name analysis)
                  [:input.btn.danger
                   {:value "Delete" :name "action" :type "submit"}])]])]]])

(defpartial analysis
  [doc & opts]
  (let [all-analysis (filter #(and (or (= "comparative" (:paramstype doc))
                                       (= (:paramstype doc) (:resultstype %)))
                                   (= (:type doc) (:run-or-sim %)))
                             (get (list-analysis) (:problem doc)))
        run (if (= "run" (:type doc)) doc (get-doc (:runid doc)))
        active-analysis (set (map get-doc (get run (if (= "run" (:type doc))
                                                     :analysis :simulation-analysis))))]
    [:section#analysis
     [:div.page-header
      [:a {:name "analysis"}
       [:h2 "Analysis"]]]
     (if (empty? active-analysis)
       [:div.row
        [:div.span16.columns [:p "No analysis."]]]
       (for [a (sort-by :name active-analysis) :when a]
         (show-analysis doc a)))
     (if-not (or (empty? all-analysis) (some #{:no-select} opts))
       [:div
        [:div.row
         [:div.span4.columns
          [:p [:b [:a.fields_checkboxes_header "Choose analysis..."]]]]]
        [:div.fields_checkboxes
         [:div.row
          [:div.span12.columns
           (form-to
            [:post "/analysis/set-analysis"]
            (hidden-field :docid (:_id doc))
            (hidden-field :runid (:_id run))
            (hidden-field :run-or-sim (:type doc))
            [:div.clearfix
             [:div.input
              [:ul.inputs-list
               (for [a all-analysis]
                 [:li [:label
                       [:input {:type "checkbox" :name "analysis[]" :value (:_id a)
                                :checked (active-analysis a)}]
                       " " (:name a)]])]]
             [:div.actions
              [:input.btn.primary {:value "Update" :type "submit"}]]])]]]])]))

(defpage
  [:post "/analysis/set-analysis"] {:as analysis}
  (set-analysis (:runid analysis) (:analysis analysis) (:run-or-sim analysis))
  (resp/redirect (format "/%s/%s#analysis" (:run-or-sim analysis) (:docid analysis))))

(defpage
  [:post "/analysis/update-analysis"] {:as analysis}
  (cond (= "Update" (:action analysis))
        (do
          (update-analysis analysis)
          (resp/redirect "/analysis"))
        (= "Delete" (:action analysis))
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/analysis/delete-analysis-confirm" (:id analysis)
                                  "Are you sure you want to delete the analysis?"))
        :else
        (resp/redirect "/analysis")))

(defpage
  [:post "/analysis/delete-analysis-confirm"] {:as confirm}
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-analysis (:id confirm))
      (resp/redirect "/analysis"))
    (resp/redirect (format "/analysis#%s" (:id confirm)))))

(defpage
  [:post "/analysis/new-analysis"] {:as analysis}
  (new-analysis analysis)
  (resp/redirect "/analysis"))

(defpage "/analysis/update/:id" {id :id}
  (let [analysis (get-doc id)]
    (common/layout
     (format "Update analysis %s" (:name analysis))
     (analysis-form analysis))))

(defpage "/analysis" {}
  (let [analysis (list-analysis)]
    (common/layout
     "Analysis"
     (for [problem (sort (keys analysis))]
       [:section {:id problem}
        [:div.row
         [:div.span16.columns
          [:div.page-header
           [:a {:name (str/replace problem #"\W" "_")}
            [:h1 (format "%s analysis" problem)]]]]]
        (for [analysis (get analysis problem)]
          [:div.row
           [:div.span4.columns
            [:a {:name (str/replace (:name analysis) #"\W" "_")}
             [:h2 (:name analysis) [:br]
              [:small (format " (%s, %s)" (:run-or-sim analysis) (:resultstype analysis))]]]
            [:p (:caption analysis)]
            [:p (link-to (format "/analysis/update/%s" (:_id analysis))
                         "Update analysis")]]
           [:div.span12.columns
            [:pre (:code analysis)]]])])
     (analysis-form {}))))
