(ns geppettoweb.views.analyses
  (:require [geppettoweb.views.common :as common])
  (:require [ring.util.response :as resp])
  (:require [clojure.string :as str])
  (:require [clojure.set :as set])
  (:use [geppettoweb.models.analyses :exclude [analyses]])
  (:use compojure.core hiccup.def hiccup.element hiccup.form hiccup.util))

(defhtml analysis-form
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

(defhtml template-analysis-fields
  [analysis id comparative-fields control-fields]
  (let [selected (get analysis id)]
    [:select {:name id :id id}
     (concat (select-options ["None"] selected)
             (if (not-empty comparative-fields)
               [[:optgroup {:label "Comparative fields"}
                 (select-options (map name comparative-fields) selected)]]
               [])
             [[:optgroup {:label "Control fields"}
               (select-options (map name control-fields) selected)]])]))

(defhtml template-analysis-form
  [run analysis comparative-fields control-fields]
  [:form.form-horizontal {:method "POST" :action (if (:name analysis) "/analyses/update-template-analysis"
                                                     "/analyses/new-template-analysis")}
   (hidden-field :runid (:runid run))
   (hidden-field :templateid (:templateid analysis))
   [:div.control-group
    [:label.control-label {:for "template"} "Template"]
    [:div.controls
     (drop-down :template ["linear-model"]
                (:template analysis))]]
   [:div.control-group
    [:label.control-label {:for "name"} "Name"]
    [:div.controls [:input.input-large {:id "name" :name "name" :size 30
                                         :type "text" :value (:name analysis)}]]]
   [:div.control-group
    [:label.control-label {:for "caption"} "Caption"]
    [:div.controls
     [:textarea.input-xxlarge {:id "caption" :name "caption"} (:caption analysis)]]]
   [:div.control-group
    [:label.control-label {:for "xfield"} "X field"]
    [:div.controls (template-analysis-fields
                    analysis :xfield comparative-fields control-fields)]]
   [:div.control-group
    [:label.control-label {:for "yfield"} "Y field"]
    [:div.controls (template-analysis-fields
                    analysis :yfield comparative-fields control-fields)]]
   [:div.form-actions
    [:input.btn.btn-primary
     {:name "action" :value (if (:name analysis) "Update" "Create")
      :type "submit"}]
    " "
    (if (:name analysis)
      [:input.btn.btn-danger
       {:value "Delete" :name "action" :type "submit"}])]])

(defhtml show-analysis
  [run analysis comparative-fields control-fields]
  [:div
   [:a {:name (if (:templateid analysis)
                (format "templateanalysis%d" (:templateid analysis))
                (format "analysis%d" (:analysisid analysis)))}
    [:h2 (format "%s%s" (:name analysis)
            (if (:templateid analysis) (format " (template %s)" (:template analysis)) ""))]]
   [:p (:caption analysis)]
   [:p
    (let [output (get-analysis-output run analysis)]
      (if (re-find #"<table" output) output [:pre output]))
    (if (:templateid analysis)
      [:div
       [:p
        [:a.code_header "Code"] " / "
        [:a.update_header "Update"]]
       [:pre.code (:code analysis)]
       [:div]
       [:div.update (template-analysis-form run analysis comparative-fields control-fields)]]
      [:div
       [:p
        [:a.code_header "Code"] " / "
        (link-to (format "/analyses/update/%s" (:analysisid analysis)) "Update")]
       [:pre.code (:code analysis)]])]])

(defhtml analyses
  [run comparative-fields control-fields]
  (let [avail-analyses (filter #(or (:comparison run)
                               (= "non-comparative" (:resultstype %)))
                          (get (list-analyses) (:problem run)))
        active-analyses (set/union (set (get-run-analyses (:runid run)))
                               (set (get-run-template-analyses (:runid run))))]
    [:section
     [:div.page-header
      [:a {:name "analyses"}
       [:h1 "Analyses"]]]
     (if (empty? active-analyses)
       [:p "No analyses."]
       (for [a (sort-by :name active-analyses) :when a]
         (show-analysis run a comparative-fields control-fields)))
     (if (not-empty avail-analyses)
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
           [:input.btn.btn-primary {:value "Update" :type "submit"}]])]])
     [:div
      [:div.row-fluid
       [:div.span12.columns
        [:p [:b [:a.new_template_form_header "New template analysis..."]]]]]
      [:div.new_template_form
       [:div.row-fluid
        [:div.span12.columns
         (template-analysis-form run {} comparative-fields control-fields)]]]]]))

(defn update-analysis-action
  [analysisid action analysis]
  (cond (= "Update" action)
        (do
          (update-analysis analysis)
          (resp/redirect (format "/analyses#analysis%s" analysisid)))
        (= "Delete" action)
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/analyses/delete-analysis-confirm" analysisid
                                  "Are you sure you want to delete the analysis?"))
        :else
        (resp/redirect "/analyses")))

(defn delete-analysis-confirm
  [id choice]
  (if (= choice "Confirm deletion")
    (do
      (delete-analysis id)
      (resp/redirect "/analyses"))
    (resp/redirect "/analyses")))

(defn update-template-analysis-action
  [runid templateid action analysis]
  (cond (= "Update" action)
        (do
          (update-template-analysis analysis)
          (resp/redirect (format "/run/%s#templateanalysis%s" runid templateid)))
        (= "Delete" action)
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/analyses/delete-template-analysis-confirm" templateid
                                  "Are you sure you want to delete the analysis?"))
        :else
        (resp/redirect (format "/run/%s" runid))))

(defn delete-template-analysis-confirm
  [id choice]
  (let [runid (get-run-for-template-analysis id)]
    (if (= choice "Confirm deletion")
      (do
        (delete-template-analysis id)
        (resp/redirect (format "/run/%d" runid)))
      (resp/redirect (format "/run/%d" runid)))))

(defn show-all-analyses []
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

(defroutes analyses-routes
  (context "/analyses" []
    (POST "/set-run-analyses" [runid analysisids]
      (do (set-run-analyses runid analysisids)
          (resp/redirect (format "/run/%s#analysis" runid))))
    (POST "/update-analysis" [analysisid action :as {analysis :params}]
      (update-analysis-action analysisid action analysis))
    (POST "/delete-analysis-confirm" [id choice]
      (delete-analysis-confirm id choice))
    (POST "/new-analysis" [:as {analysis :params}]
      (let [analysisid (new-analysis analysis)]
        (resp/redirect (format "/analyses#analysis%d" analysisid))))
    (POST "/update-template-analysis" [runid templateid action :as {analysis :params}]
      (update-template-analysis-action runid templateid action analysis))
    (POST "/delete-template-analysis-confirm" [id choice]
      (delete-template-analysis-confirm id choice))
    (POST "/new-template-analysis" [:as {analysis :params}]
      (let [templateid (new-template-analysis analysis)]
        (resp/redirect (format "/run/%s#templateanalysis%d" (:runid analysis) templateid))))
    (GET "/update/:analysisid" [analysisid]
      (let [analysis (get-analysis analysisid)]
        (common/layout
         (format "Update %s" (:name analysis))
         (analysis-form analysis))))
    (GET "/" [] (show-all-analyses))))

