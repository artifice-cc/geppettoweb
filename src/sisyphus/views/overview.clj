(ns sisyphus.views.overview
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:require [noir.cookies :as cookies])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.runs :only
         [problem-fields list-runs summarize-comparative-results]]))

(defpartial run-table-row
  [run summary]
  (let [id (:id run)
        r (:value run)]
    [:tr
     [:td (link-to (format "/details/%s" id) (subs id 0 10))]
     [:td (common/date-format (:time r))]
     [:td (common/strategy-format (:control-strategy r))]
     [:td (common/strategy-format (:comparison-strategy r))]
     [:td (if (not-empty (:rows summary))
            (format "%.2f" ((comp double :value first :rows) summary))
            "N/A")]
     [:td (:control-count r)] [:td (:comparison-count r)]
     [:td (:comparative-count r)]
     [:td (link-to (format "https://github.com/joshuaeckroth/retrospect/commit/%s" (:commit r))
                   (subs (:commit r) 0 10))]]))

(defpartial runs-table
  [runs problem custom]
  (let [summaries (sort-by #(if (not-empty (:rows (second %)))
                              ((comp double :value first :rows second) %) Double/NEGATIVE_INFINITY)
                           (map (fn [r] [r (eval (summarize-comparative-results (:id r) custom))])
                                runs))]
    [:table.tablesorter
     [:thead
      [:tr
       [:th "Run ID"]
       [:th "Time"]
       [:th "Control strategy"] [:th "Comparison strategy"]
       [:th (format "%s (%s,%s)" (:field custom) (:order custom) (:func custom))]
       [:th "Control"] [:th "Comparison"] [:th "Comparative"]
       [:th "Commit"]]]
     [:tbody
      (map (fn [[run summary]] (run-table-row run summary))
           (if (= "DESC" (:order custom)) (reverse summaries) summaries))]]))

(defpartial runs
  [problem runs]
  (let [fields (problem-fields problem)
        custom-field (or (cookies/get (keyword (format "%s-field" problem))) (first fields))
        custom-order (or (cookies/get (keyword (format "%s-order" problem))) "ASC")
        custom-func (or (cookies/get (keyword (format "%s-func" problem))) "SUM")]
    [:section {:id (format "runs-%s" problem)}
     [:div.page-header
      [:h1 problem]]
     [:div.row
      [:div.span16.columns
       (runs-table runs problem {:field custom-field :order custom-order :func custom-func})]]
     [:div.row
      [:div.span4.columns
       [:h2 "Summarization"]]
      [:div.span12.columns
       (form-to [:post "/set-custom"]
                (hidden-field :problem problem)
                [:div.clearfix
                 [:label {:for "field"} "Field"]
                 [:div.input
                  (drop-down :field fields custom-field)]]
                [:div.clearfix
                 [:label {:for "order"} "Order"]
                 [:div.input
                  (drop-down :order ["ASC" "DESC"] custom-order)]]
                [:div.clearfix
                 [:label {:for "func"} "Function"]
                 [:div.input
                  (drop-down :func ["AVG" "SUM" "MAX" "MIN"] custom-func)]]
                [:div.actions
                 [:input.btn.primary {:value "Update" :type "submit"}]])]]]))

(defpartial runs-by-problem
  [runs-grouped]
  (map (fn [problem] (runs problem (get runs-grouped problem)))
       (keys runs-grouped)))

(defpage
  [:post "/set-custom"] {:as custom}
  (cookies/put! (keyword (format "%s-field" (:problem custom))) (:field custom))
  (cookies/put! (keyword (format "%s-order" (:problem custom))) (:order custom))
  (cookies/put! (keyword (format "%s-func" (:problem custom))) (:func custom))
  (resp/redirect "/"))

(defpage "/" []
  (let [runs-grouped (group-by (comp :problem :value) (list-runs))]
    (common/layout "Overview" (runs-by-problem runs-grouped))))
