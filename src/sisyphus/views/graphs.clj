(ns sisyphus.views.graphs
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use [ring.util.response :only [header]])
  (:require [clojure.string :as str])
  (:use [sisyphus.models.graphs :only
         [list-graphs get-graph
          get-run-graphs set-run-graphs
          new-graph new-template-graph update-graph delete-graph
          render-graph-file get-graph-png get-graph-download]])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(def graph-help (.markdown common/mdp
                           (slurp "/home/josh/git/research/sisyphus/help/graphs.md")))

(defpartial show-graph
  [run graph & opts]
  (let [widthpx (int (* 100 (:width graph)))
        heightpx (int (* 100 (:height graph)))]
    [:div
     [:div.row
      [:div.span12.columns
       [:a {:name (format "graph%d" (:graphid graph))}
        [:h3 (:name graph)]]
       [:p (:caption graph)]]]
     [:div.row
      [:div.span12.columns
       [:p
        (if-let [err (:err (render-graph-file run graph "png" "website"
                                              (:width graph) (:height graph)))]
          [:div
           [:pre err]
           [:p
            [:a.code_header "Code"] " / "
            (link-to (format "/graphs/update/%s" (:graphid graph)) "Update")]
           [:pre.code {:style (format "width: %dpx;" widthpx)} (:code graph)]]        
          [:div
           [:img {:src (format "/graph/%s/%s/png" (:runid run) (:graphid graph))
                  :width widthpx
                  :height heightpx}]
           (if (not (some #{:no-select} opts))
             [:div
              [:p
               [:a.code_header "Code"] " / "
               (link-to (format "/graphs/update/%s" (:graphid graph)) "Update")
               " / "
               [:a.download_header "Download"]]
              [:pre.code {:style (format "width: %dpx;" widthpx)} (:code graph)]
              [:div.download
               (form-to [:post "/graph/download"]
                        (hidden-field :runid (:runid run))
                        (hidden-field :graphid (:graphid graph))
                        [:fieldset
                         [:div.clearfix
                          [:label {:for "theme"} "Theme"]
                          [:div.input
                           (drop-down :theme ["paper" "poster" "website"])]]
                         [:div.clearfix
                          [:label {:for "width"} "Width (in)"]
                          [:div.input
                           [:input.xlarge {:id "width" :name "width"
                                           :size 3 :type "text" :value (:width graph)}]]]
                         [:div.clearfix
                          [:label {:for "height"} "Height (in)"]
                          [:div.input
                           [:input.xlarge {:id "height" :name "height"
                                           :size 3 :type "text" :value (:height graph)}]]]
                         [:div.clearfix
                          [:label {:for "filename"} "File name (without extension)"]
                          [:div.input
                           [:input.xlarge
                            {:id "filename" :name "filename" :size 30 :type "text"
                             :value (format "%s-%s-%d--%s"
                                       (:problem run) (:name run) (:runid run)
                                       (:name graph))}]]]
                         [:div.actions
                          [:input.btn
                           {:name "ftype" :value "png" :type "submit"}]
                          " "
                          [:input.btn
                           {:name "ftype" :value "pdf" :type "submit"}]
                          " "
                          [:input.btn
                           {:name "ftype" :value "svg" :type "submit"}]]])]])])]]]]))

(defpartial graph-form
  [graph]
  [:section#graph-form
   [:div.page-header
    [:a {:name "new"}
     [:h1 (if (:name graph) (format "Update graph %s" (:name graph))
              "New graph")]]]
   [:div.row
    [:div.span12.columns
     (form-to [:post (if (:name graph) "/graphs/update-graph"
                         "/graphs/new-graph")]
              (hidden-field :graphid (:graphid graph))
              [:fieldset
               [:div.clearfix
                [:label {:for "problems"} "Problems"]
                [:div.input
                 [:input.xlarge {:id "problems" :name "problems" :size 30
                                 :type "text" :value (:problems graph)}]]]
               [:div.clearfix
                [:label {:for "name"} "Name"]
                [:div.input
                 [:input.xlarge {:id "name" :name "name" :size 30
                                 :type "text" :value (:name graph)}]]]
               [:div.clearfix
                [:label {:for "width"} "Width (inches)"]
                [:div.input
                 [:input.xlarge {:id "width" :name "width" :size 30
                                 :type "text" :value (:width graph)}]]]
               [:div.clearfix
                [:label {:for "height"} "Height (inches)"]
                [:div.input
                 [:input.xlarge {:id "height" :name "height" :size 30
                                 :type "text" :value (:height graph)}]]]
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
                 [:textarea.xxlarge {:id "code" :name "code" :rows 30
                                     :style "font-family: monospace;"}
                  (if (:code graph) (:code graph) "")]
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
                   {:value "Delete" :name "action" :type "submit"}])]])]]
   [:div.row
    [:div.span12.columns
     [:a {:name "help"}
      [:h1 "Help"]]]]
   [:div.row
    [:div.span12.columns graph-help]]])

(defpartial template-graph-fields
  [id comparative-fields control-fields]
  (comment
    [:select {:name id :id id}
     (concat (if (#{:x-factor :metric} id) [] (select-options ["None"]))
             (if (not-empty comparative-fields)
               [[:optgroup {:label "Comparative fields"}
                 (select-options (map name comparative-fields))]]
               [])
             [[:optgroup {:label "Control fields"}
               (select-options (map name control-fields))]])]))

(defpartial template-graph-form
  [doc template-graph comparative-fields control-fields]
  (comment
    (form-to [:post (if (:name template-graph) "/graph/update-template-graph"
                        "/graphs/new-template-graph")]
             (hidden-field :docid (:_id doc))
             (hidden-field :graphid (:_id template-graph))
             (hidden-field :graphrev (:_rev template-graph))
             (hidden-field :name "template-graph")
             [:fieldset
              [:div.clearfix
               [:label {:for "template"} "Template"]
               [:div.input
                (drop-down :template ["bars"])]]
              [:div.clearfix
               [:label {:for "x-factor"} "x-factor"]
               [:div.input (template-graph-fields :x-factor comparative-fields
                                                  control-fields)]]
              [:div.clearfix
               [:label {:for "x-axis"} "x-axis label"]
               [:div.input [:input.xlarg {:id "x-axis" :name "x-axis" :size 30
                                          :type "text"}]]]
              [:div.clearfix
               [:label {:for "metric"} "Metric"]
               [:div.input (template-graph-fields :metric comparative-fields
                                                  control-fields)]]
              [:div.clearfix
               [:label {:for "y-axis"} "y-axis label"]
               [:div.input [:input.xlarg {:id "y-axis" :name "y-axis" :size 30
                                          :type "text"}]]]
              [:div.clearfix
               [:label {:for "fill"} "Fill"]
               [:div.input (template-graph-fields :fill comparative-fields
                                                  control-fields)]]
              [:div.clearfix
               [:label {:for "facet-horiz"} "Facet horizontal"]
               [:div.input (template-graph-fields :facet-horiz comparative-fields
                                                  control-fields)]]
              [:div.clearfix
               [:label {:for "facet-vert"} "Facet vertical"]
               [:div.input (template-graph-fields :facet-vert comparative-fields
                                                  control-fields)]]]
             [:div.actions
              [:input.btn.primary
               {:name "action" :value (if (:name template-graph) "Update" "Save")
                :type "submit"}]
              " "
              (if (:name template-graph)
                [:input.btn.danger
                 {:value "Delete" :name "action" :type "submit"}])])))

(defpartial graphs
  [run comparative-fields control-fields & opts]
  (let [avail-graphs (filter #(if (nil? (:comparison run))
                           (= "non-comparative" (:resultstype %)))
                        (get (list-graphs) (:problem run)))
        active-graphs (set (get-run-graphs (:runid run)))
        template-graphs #{}]
    (comment
      [:div.row
       [:div.span12.columns
        (apply show-graph doc (first template-graphs) opts)]]
      (println template-graphs))
    (if (or (not-empty avail-graphs) (not (some #{:no-select} opts)))
      [:section#graphs
       [:div.page-header
        [:a {:name "graphs"}
         [:h2 "Graphs"]]]
       (if (empty? active-graphs)
         [:div.row
          [:div.span12.columns [:p "No graphs."]]]
         (for [g (sort-by :name active-graphs) :when g]
           (apply show-graph run g opts)))
       (if-not (or (empty? avail-graphs) (some #{:no-select} opts))
         [:div
          [:div.row
           [:div.span4.columns
            [:p [:b [:a.fields_checkboxes_header "Choose graphs..."]]]]]
          [:div.fields_checkboxes
           [:div.row
            [:div.span8.columns
             (form-to
              [:post "/graphs/set-run-graphs"]
              (hidden-field :runid (:runid run))
              [:div.clearfix
               [:div.input
                [:ul.inputs-list
                 (for [g (sort-by :name avail-graphs)]
                   [:li [:label
                         [:input {:type "checkbox" :name "graphids[]" :value (:graphid g)
                                  :checked (active-graphs g)}]
                         " " (:name g)]])]]
               [:div.actions
                [:input.btn.primary {:value "Update" :type "submit"}]]])]]]
          [:div.row
           [:div.span12.columns
            (template-graph-form run {} comparative-fields control-fields)]]])])))

(defpage
  [:post "/graphs/set-run-graphs"] {:as graphs}
  (set-run-graphs (:runid graphs) (:graphids graphs))
  (resp/redirect (format "/run/%s#graphs" (:runid graphs))))

(defpage
  [:post "/graphs/update-graph"] {:as graph}
  (cond (= "Update" (:action graph))
        (do
          (update-graph graph)
          (resp/redirect (format "/graphs#graph%s" (:graphid graph))))
        (= "Delete" (:action graph))
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/graphs/delete-graph-confirm" (:graphid graph)
                                  "Are you sure you want to delete the graph?"))
        :else
        (resp/redirect "/graphs")))

(defpage
  [:post "/graphs/delete-graph-confirm"] {:as confirm}
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-graph (:id confirm))
      (resp/redirect "/graphs"))
    (resp/redirect "/graphs")))

(defpage
  [:post "/graphs/new-graph"] {:as graph}
  (let [graphid (new-graph graph)]
    (resp/redirect (format "/graphs#graph%d" graphid))))

(defpage
  [:post "/graphs/new-template-graph"] {:as template-graph}
  (comment
    (new-template-graph template-graph)
    (resp/redirect (format "/graphs#%s" (get-template-graph-anchor template-graph)))))

(defpage "/graphs/update/:graphid" {graphid :graphid}
  (let [graph (get-graph graphid)]
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
         [:div.span12.columns
          [:div.page-header
           [:a {:name (str/replace problem #"\W" "_")}
            [:h1 (format "%s graphs" problem)]]]]]
        (for [graph (sort-by :name (get graphs problem))]
          [:div.row
           [:div.span4.columns
            [:a {:name (format "graph%d" (:graphid graph))}
             [:h2 (:name graph) [:br]
              [:small (format "%s<br/>(%.1f by %.1f inches)<br/>(%s)"
                         (:problems graph) (:width graph) (:height graph)
                         (:resultstype graph))]]]
            [:p (:caption graph)]
            [:p (link-to (format "/graphs/update/%s" (:graphid graph))
                         "Update graph")]]
           [:div.span8.columns
            [:pre (:code graph)]]])])
     (graph-form {}))))

(defpage "/graph/:runid/:graphid/png"
  {runid :runid graphid :graphid}
  (resp/content-type "image/png" (get-graph-png (Integer/parseInt runid)
                                                (Integer/parseInt graphid))))

(defpage [:post "/graph/download"] {:as graph}
  (->
   (resp/content-type (cond (= "pdf" (:ftype graph))
                            "application/pdf"
                            (= "svg" (:ftype graph))
                            "image/svg+xml"
                            (= "png" (:ftype graph))
                            "image/png")
                      (get-graph-download (Integer/parseInt (:runid graph))
                                          (Integer/parseInt (:graphid graph))
                                          (:ftype graph)
                                          (:theme graph)
                                          (Double/parseDouble (:width graph))
                                          (Double/parseDouble (:height graph))))
   (header "Content-Disposition"
           (format "attachment; filename=\"%s.%s\"" (:filename graph) (:ftype graph)))))
