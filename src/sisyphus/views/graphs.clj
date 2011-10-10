(ns sisyphus.views.graphs
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.graphs :only [list-graphs new-graph update-graph get-graph-png]])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(defpartial show-graph
  [run graph]
  [:div.row
   [:div.span4.columns
    [:h3 (:name graph) [:small (format " (%s)" (:results-type graph))]]
    [:p (:caption graph)]]
   [:div.span8.columns
    [:p
     [:img {:src (format "/graph/%s/%s/%s" (:_id run) (:_id graph) (:_rev graph))
            :width 700 :height 400}]]
    [:pre {:style "width: 650px;"} (:code graph)]]])

(comment [:div.row
          [:div.span16.columns
           [:p (format "Failed to produce graph %s" (:name graph))]
           [:pre (:err png)]]])

(defpartial graph-form
  [graph]
  [:section#graph-form
   [:div.page-header
    [:h1 (if (:name graph) (format "Update graph %s" (:name graph))
             "New graph")]]
   [:div.row
    [:div.span4.columns
     [:h2 "Metadata"]]
    [:div.span12.columns
     (form-to [:post (if (:name graph) "/graphs/update-graph" "/graphs/new-graph")]
              (hidden-field :id (:_id graph))
              [:fieldset
               [:legend "Metadata"]
               [:div.clearfix
                [:label {:for "problem"} "Problem"]
                [:div.input
                 [:input.xlarge {:id "problem" :name "problem" :size 30
                                 :type "text" :value (:problem graph)}]]]
               [:div.clearfix
                [:label {:for "name"} "Name"]
                [:div.input
                 [:input.xlarge {:id "name" :name "name" :size 30
                                 :type "text" :value (:name graph)}]]]
               [:div.clearfix
                [:label {:for "results-type"} "Results type"]
                [:div.input
                 (drop-down :results-type ["control/comparison" "comparative"]
                            (:results-type graph))]]
               [:div.clearfix
                [:label {:for "caption"} "Caption"]
                [:div.input
                 [:textarea.xxlarge {:id "caption" :name "caption"} (:caption graph)]]]
               [:div.clearfix
                [:label {:for "code"} "R code"]
                [:div.input
                 [:textarea.xxlarge {:id "code" :name "code"}
                  (if (:code graph) (:code graph)
                      "p <- ggplot(comparative) + geom_point(aes(x=Field1, y=Field2))")]
                 [:span.help-block "Assume the existence of data tables named 'control',
                                     'comparison', and 'comparative'
                                     and that 'ggplot2' is loaded.
                                     Save the graph to the variable 'p'."]]]
               [:div.actions
                [:input.btn.primary {:value (if (:name graph) "Update" "Save") :type "submit"}]]])]]])

(defpage
  [:post "/graphs/update-graph"] {:as graph}
  (update-graph graph)
  (resp/redirect "/graphs"))

(defpage
  [:post "/graphs/new-graph"] {:as graph}
  (new-graph graph)
  (resp/redirect "/graphs"))

(defpage "/graphs/update/:id" {id :id}
  (let [graph (get-doc id)]
    (common/layout
     (format "Update graph %s" (:name graph))
     (graph-form graph))))

(defpage "/graphs" {}
  (let [graphs (list-graphs)]
    (common/layout
     "Graphs"
     (for [problem (sort (keys graphs))]
       [:section {:id problem}
        [:div.row
         [:div.span16.columns
          [:div.page-header [:h1 (format "%s graphs" problem)]]]]
        (for [graph (get graphs problem)]
          [:div.row
           [:div.span4.columns
            [:h2 (:name graph) [:small (format " (%s)" (:results-type graph))]]
            [:p (:caption graph)]
            [:p (link-to (format "/graphs/update/%s" (:_id graph)) "Update graph")]]
           [:div.span12.columns
            [:pre (:code graph)]]])])
     (graph-form {}))))

(defpage "/graph/:runid/:graphid/:graphrev"
  {runid :runid graphid :graphid graphrev :graphrev}
  (resp/content-type "image/png" (get-graph-png runid graphid graphrev)))
