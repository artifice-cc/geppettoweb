(ns sisyphus.views.analyses
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:require [clojure.string :as str])
  (:use [sisyphus.models.analyses :only
         [list-analyses get-analysis new-analysis update-analysis delete-analysis
          get-run-analyses set-run-analyses get-analysis-output]])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(defpartial show-analysis
  [run analysis]
  [:div
   [:a {:name (format "analysis%d" (:analysisid analysis))}
    [:h3 (:name analysis)]]
   [:pre (get-analysis-output run analysis)]])

(defpartial analysis-form
  [analysis]
  [:section#analysis-form
   [:div.page-header
    (if (:name analysis)
      [:h1 (format "Update %s" (:name analysis))]
      [:a {:name "new"}
       [:h1 "New analysis"]])]
   [:form.form-horizontal {:method "POST" :action (if (:name analysis) "/analyses/update-analysis"
                                                      "/analyses/new-analysis")}
    (hidden-field :analysisid (:analysisid analysis))
    [:div.control-group
     [:label.control-label {:for "problems"} "Problems"]
     [:div.controls
      [:input.input-large {:id "problems" :name "problems" :size 30
                           :type "text" :value (:problems analysis)}]]]
    [:div.control-group
     [:label.control-label {:for "name"} "Name"]
     [:div.controls
      [:input.input-large {:id "name" :name "name" :size 30
                           :type "text" :value (:name analysis)}]]]
    [:div.control-group
     [:label.control-label {:for "resultstype"} "Results type"]
     [:div.controls
      (drop-down :resultstype ["non-comparative" "comparative"]
                 (:resultstype analysis))]]
    [:div.control-group
     [:label.control-label {:for "caption"} "Caption"]
     [:div.controls
      [:textarea.input-xxlarge {:id "caption" :name "caption"} (:caption analysis)]]]
    [:div.control-group
     [:label.control-label {:for "code"} "R code"]
     [:div.controls
      [:textarea.input-xxlarge {:id "code" :name "code" :rows 30
                                :style "font-family: monospace;"}
       (if (:code analysis) (:code analysis) "")]
      [:span.help-block "Assume the existence of data frames named 'control',
                                    'comparison', and 'comparative'."]]]
    [:div.form-actions
     [:input.btn.btn-primary
      {:name "action" :value (if (:name analysis) "Update" "Save")
       :type "submit"}]
     " "
     (if (:name analysis)
       [:input.btn.btn-danger
        {:value "Delete" :name "action" :type "submit"}])]]])

(defpartial analyses
  [run & opts]
  (let [avail-analyses (filter #(or (:comparison run)
                               (= "non-comparative" (:resultstype %)))
                          (get (list-analyses) (:problem run)))
        active-analyses (set (get-run-analyses (:runid run)))]
    (if (or (not-empty active-analyses) (not (some #{:no-select} opts)))
      [:section
       [:div.page-header
        [:a {:name "analyses"}
         [:h1 "Analyses"]]]
       (if (empty? active-analyses)
         [:p "No analyses."]
         (for [a (sort-by :name active-analyses) :when a]
           (show-analysis run a)))
       (if-not (or (empty? avail-analyses) (some #{:no-select} opts))
         [:div
          [:div.row-fluid
           [:div.span12.columns
            [:p [:b [:a.fields_checkboxes_header "Choose analyses..."]]]]]
          [:div.fields_checkboxes
           (form-to
            [:post "/analyses/set-run-analyses"]
            (hidden-field :runid (:runid run))
            [:div.row-fluid
             (for [analyses-group (partition-all (int (Math/ceil (/ (count avail-analyses) 2)))
                                                 (sort-by :name avail-analyses))]
               [:div.span6
                (for [a analyses-group]
                  [:label.checkbox
                   [:input {:type "checkbox" :name "analysisids[]" :value (:analysisid a)
                            :checked (active-analyses a)}]
                   " " (:name a)])])]
            [:div.form-actions
             [:input.btn.btn-primary {:value "Update" :type "submit"}]])]])])))

(defpage
  [:post "/analyses/set-run-analyses"] {:as analyses}
  (set-run-analyses (:runid analyses) (:analysisids analyses))
  (resp/redirect (format "/run/%s#analysis" (:runid analyses))))

(defpage
  [:post "/analyses/update-analysis"] {:as analysis}
  (cond (= "Update" (:action analysis))
        (do
          (update-analysis analysis)
          (resp/redirect (format "/analyses#analysis%s" (:analysisid analysis))))
        (= "Delete" (:action analysis))
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/analyses/delete-analysis-confirm" (:analysisid analysis)
                                  "Are you sure you want to delete the analysis?"))
        :else
        (resp/redirect "/analyses")))

(defpage
  [:post "/analyses/delete-analysis-confirm"] {:as confirm}
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-analysis (:id confirm))
      (resp/redirect "/analyses"))
    (resp/redirect "/analyses")))

(defpage
  [:post "/analyses/new-analysis"] {:as analysis}
  (let [analysisid (new-analysis analysis)]
    (resp/redirect (format "/analyses#analysis%d" analysisid))))

(defpage "/analyses/update/:analysisid" {analysisid :analysisid}
  (let [analysis (get-analysis analysisid)]
    (common/layout
     (format "Update analysis %s" (:name analysis))
     (analysis-form analysis))))

(defpage "/analyses" {}
  (let [analyses (list-analyses)]
    (common/layout
     "Analyses"
     (for [problem (sort (keys analyses))]
       [:section {:id problem}
        [:div.page-header
         [:a {:name (str/replace problem #"\W" "_")}
          [:h1 (format "%s analyses" problem)]]]
        (for [analysis (sort-by :name (get analyses problem))]
          [:div.row-fluid
           [:div.span4.columns
            [:a {:name (format "analysis%d" (:analysisid analysis))}
             [:h2 (:name analysis) [:br]
              [:small (format "%s<br/>(%s)"
                         (:problems analysis) (:resultstype analysis))]]]
            [:p (:caption analysis)]
            [:p (link-to (format "/analyses/update/%s" (:analysisid analysis))
                         "Update analysis")]]
           [:div.span8.columns
            [:pre (:code analysis)]]])])
     (analysis-form {}))))
