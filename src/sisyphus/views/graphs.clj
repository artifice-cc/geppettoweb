(ns sisyphus.views.graphs
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use [sisyphus.models.graphs :only [list-graphs new-graph]])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(defpartial new-graph-form []
  [:section#new-graph
   [:div.page-header
    [:h1 (format "New graph")]]
   [:div.row
    [:div.span4.columns
     [:h2 "Metadata"]]
    [:div.span12.columns
     (form-to [:post "/graphs/new-graph"]
              [:fieldset
               [:legend "Metadata"]
               [:div.clearfix
                [:label {:for "problem"} "Problem"]
                [:div.input
                 [:input.xlarge {:id "problem" :name "problem" :size 30 :type "text"}]]]
               [:div.clearfix
                [:label {:for "name"} "Name"]
                [:div.input
                 [:input.xlarge {:id "name" :name "name" :size 30 :type "text"}]]]
               [:div.clearfix
                [:label {:for "results-type"} "Results type"]
                [:div.input
                 (drop-down :results-type ["control" "comparison" "comparative"])]]
               [:div.clearfix
                [:label {:for "code"} "R code"]
                [:div.input
                 [:textarea.xxlarge {:id "code" :name "code"}
                  "p <- ggplot(comparative) + geom_point(aes(x=Field1, y=Field2))"]
                 [:span.help-block "Assume the existence of a data table named 'control' or
                                     'comparison' or 'comparative' (depending on
                                     the results type selected above) and that 'ggplot2' is loaded.
                                     Save the graph to the variable 'p'."]]]
               [:div.actions
                [:input.btn.primary {:value "Save" :type "submit"}]
                "&nbsp;"
                [:button.btn {:type "reset"} "Cancel"]]])]]])

(defpage
  [:post "/graphs/new-graph"] {:as graph}
  (new-graph graph)
  (resp/redirect "/graphs"))

(defpage "/graphs" {}
  (let [graphs (list-graphs)]
    (common/layout
     "Graphs"
     (for [problem (keys graphs)]
       [:section {:id problem}
        [:div.row
         [:div.span16.columns
          [:div.page-header [:h1 (format "%s graphs" problem)]]]]
        (for [graph (get graphs problem)]
          [:div.row
           [:div.span4.columns
            [:h2 (:name graph)]
            [:p (format "Results type: %s" (:results-type graph))]]
           [:div.span12.columns
            [:pre (:code graph)]]])])
     (new-graph-form))))