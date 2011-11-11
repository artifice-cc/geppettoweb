(ns sisyphus.views.graphs
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.graphs :only
         [list-graphs new-graph update-graph set-graphs get-graph-png delete-graph]])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(defpartial show-graph
  [doc graph]
  [:div.row
   [:div.span4.columns
    [:h3 (:name graph) [:br]
     [:small (format " (%s, %s)" (:run-or-sim graph) (:resultstype graph))]]
    [:p (:caption graph)]]
   [:div.span8.columns
    [:p
     [:img {:src (format "/graph/%s/%s/%s" (:_id doc) (:_id graph) (:_rev graph))
            :width 700 :height 400}]]]])

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
     (form-to [:post (if (:name graph) "/graphs/update-graph"
                         "/graphs/new-graph")]
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
                [:label {:for "run-or-sim"} "Run or simulation?"]
                [:div.input
                 (drop-down :run-or-sim ["run" "simulation"]
                            (:run-or-sim graph))]]
               [:div.clearfix
                [:label {:for "resultstype"} "Results type"]
                [:div.input
                 (drop-down :resultstype ["non-comparative" "comparative"]
                            (:resultstype graph))]]
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
                 [:span.help-block "Assume the existence of a data table named
                                    'control', and tables 'comparison' and 'comparative'
                                    if the results type is comparative; also assume
                                    that that 'ggplot2' is loaded. Save the graph to
                                    the variable 'p'."]]]
               [:div.actions
                [:input.btn.primary
                 {:name "action" :value (if (:name graph) "Update" "Save")
                  :type "submit"}]
                " "
                (if (:name graph)
                  [:input.btn.danger
                   {:value "Delete" :name "action" :type "submit"}])]])]]])

(defpartial graphs
  [doc & opts]
  (let [all-graphs (filter #(and (= (:paramstype doc) (:resultstype %))
                                 (= (:type doc) (:run-or-sim %)))
                           (get (list-graphs) (:problem doc)))
        active-graphs (set (map get-doc (:graphs doc)))]
    [:section#graphs
     [:div.page-header
      [:h2 "Graphs"]]
     (if (empty? active-graphs)
       [:div.row
        [:div.span16.columns [:p "No graphs."]]]
       (for [g (sort-by :name active-graphs)]
         (show-graph doc g)))
     (if-not (or (empty? all-graphs) (some #{:no-select} opts))
       [:div.row
        [:div.span4.columns
         [:h3 "Choose graphs"]]
        [:div.span12.columns
         (form-to
          [:post "/graphs/set-graphs"]
          (hidden-field :id (:_id doc))
          (hidden-field :run-or-sim (:type doc))
          [:div.clearfix
           [:div.input
            [:ul.inputs-list
             (for [g all-graphs]
               [:li [:label
                     [:input {:type "checkbox" :name "graphs[]" :value (:_id g)
                              :checked (active-graphs g)}]
                     " " (:name g)]])]]
           [:div.actions
            [:input.btn.primary {:value "Update" :type "submit"}]]])]])]))

(defpage
  [:post "/graphs/set-graphs"] {:as graphs}
  (set-graphs (:id graphs) (:graphs graphs))
  (resp/redirect (format "/%s/%s#graphs" (:run-or-sim graphs) (:id graphs))))

(defpage
  [:post "/graphs/update-graph"] {:as graph}
  (cond (= "Update" (:action graph))
        (do
          (update-graph graph)
          (resp/redirect "/graphs"))
        (= "Delete" (:action graph))
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/graphs/delete-graph-confirm" (:id graph)
                                  "Are you sure you want to delete the graph?"))
        :else
        (resp/redirect "/graphs")))

(defpage
  [:post "/graphs/delete-graph-confirm"] {:as confirm}
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-graph (:id confirm))
      (resp/redirect "/graphs"))
    (resp/redirect (format "/graphs#%s" (:id confirm)))))

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
            [:h2 (:name graph) [:br]
             [:small (format " (%s, %s)" (:run-or-sim graph) (:resultstype graph))]]
            [:p (:caption graph)]
            [:p (link-to (format "/graphs/update/%s" (:_id graph)) "Update graph")]]
           [:div.span12.columns
            [:pre (:code graph)]]])])
     (graph-form {}))))

(defpage "/graph/:docid/:graphid/:graphrev"
  {docid :docid graphid :graphid graphrev :graphrev}
  (resp/content-type "image/png" (get-graph-png (get-doc docid)
                                                (get-doc graphid graphrev))))
