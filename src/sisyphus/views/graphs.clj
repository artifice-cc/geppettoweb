(ns sisyphus.views.graphs
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use [ring.util.response :only [header]])
  (:require [clojure.string :as str])
  (:require [clojure.set :as set])
  (:use [sisyphus.models.graphs :only
         [list-graphs get-graph
          get-run-graphs set-run-graphs get-run-template-graphs
          new-graph update-graph delete-graph
          new-template-graph update-template-graph delete-template-graph
          render-graph-file get-graph-png get-graph-download]])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(def graph-help (.markdown common/mdp
                           (slurp "/home/josh/git/research/sisyphus/help/graphs.md")))

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
                 [:input.small {:id "width" :name "width" :size 5
                                :type "text" :value (:width graph)}]]]
               [:div.clearfix
                [:label {:for "height"} "Height (inches)"]
                [:div.input
                 [:input.small {:id "height" :name "height" :size 5
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
  [graph id comparative-fields control-fields]
  (let [selected (get graph id)]
    [:select {:name id :id id}
     (concat (select-options ["None"] selected)
             (if (not-empty comparative-fields)
               [[:optgroup {:label "Comparative fields"}
                 (select-options (map name comparative-fields) selected)]]
               [])
             [[:optgroup {:label "Control fields"}
               (select-options (map name control-fields) selected)]])]))

(defpartial template-graph-form
  [run graph comparative-fields control-fields]
  (form-to [:post (if (:name graph) "/graphs/update-template-graph"
                      "/graphs/new-template-graph")]
           (hidden-field :runid (:runid run))
           (hidden-field :templateid (:templateid graph))
           [:fieldset
            [:div.clearfix
             [:label {:for "template"} "Template"]
             [:div.input
              (drop-down :template ["line" "line-comparative" "bars" "bars-comparative"]
                         (:template graph))]]
            [:div.clearfix
             [:label {:for "name"} "Name"]
             [:div.input [:input.xlarg {:id "name" :name "name" :size 30
                                        :type "text" :value (:name graph)}]]]
            [:div.clearfix
             [:label {:for "caption"} "Caption"]
             [:div.input
              [:textarea.xxlarge {:id "caption" :name "caption"} (:caption graph)]]]
            [:div.clearfix
             [:label {:for "xfield"} "X field (if no factor)"]
             [:div.input (template-graph-fields
                          graph :xfield comparative-fields control-fields)]]
            [:div.clearfix
             [:label {:for "xfactor"} "X factor (if no field)"]
             [:div.input (template-graph-fields
                          graph :xfactor comparative-fields control-fields)]]
            [:div.clearfix
             [:label {:for "xlabel"} "X label"]
             [:div.input [:input.xlarg {:id "xlabel" :name "xlabel" :size 30
                                        :type "text" :value (:xlabel graph)}]]]
            [:div.clearfix
             [:label {:for "yfield"} "Y field"]
             [:div.input (template-graph-fields
                          graph :yfield comparative-fields control-fields)]]
            [:div.clearfix
             [:label {:for "ylabel"} "Y label"]
             [:div.input [:input.xlarg {:id "ylabel" :name "ylabel" :size 30
                                        :type "text" :value (:ylabel graph)}]]]
            [:div.clearfix
             [:label {:for "fill"} "Fill"]
             [:div.input (template-graph-fields
                          graph :fill comparative-fields control-fields)]]
            [:div.clearfix
             [:label {:for "color"} "Color"]
             [:div.input (template-graph-fields
                          graph :color comparative-fields control-fields)]]
            [:div.clearfix
             [:label {:for "linetype"} "Line type"]
             [:div.input (template-graph-fields
                          graph :linetype comparative-fields control-fields)]]
            [:div.clearfix
             [:label {:for "shape"} "Shape"]
             [:div.input (template-graph-fields
                          graph :shape comparative-fields control-fields)]]
            [:div.clearfix
             [:label {:for "facethoriz"} "Facet horizontal"]
             [:div.input (template-graph-fields
                          graph :facethoriz comparative-fields control-fields)]]
            [:div.clearfix
             [:label {:for "facetvert"} "Facet vertical"]
             [:div.input (template-graph-fields
                          graph :facetvert comparative-fields control-fields)]]]
           [:div.clearfix
            [:label {:for "width"} "Width (inches)"]
            [:div.input
             [:input.small {:id "width" :name "width" :size 5
                            :type "text" :value (or (:width graph) "7.0")}]]]
           [:div.clearfix
            [:label {:for "height"} "Height (inches)"]
            [:div.input
             [:input.small {:id "height" :name "height" :size 5
                            :type "text" :value (or (:height graph) "4.0")}]]]
           [:div.actions
            [:input.btn.primary
             {:name "action" :value (if (:name graph) "Update" "Save")
              :type "submit"}]
            " "
            (if (:name graph)
              [:input.btn.danger
               {:value "Delete" :name "action" :type "submit"}])]))

(defpartial graph-download-form
  [run graph]
  (form-to [:post "/graph/download"]
           (hidden-field :runid (:runid run))
           (hidden-field :graphid (:graphid graph))
           (hidden-field :templateid (:templateid graph))
           [:fieldset
            [:div.clearfix
             [:label {:for "theme"} "Theme"]
             [:div.input
              (drop-down :theme ["paper" "poster" "website"])]]
            [:div.clearfix
             [:label {:for "width"} "Width (in)"]
             [:div.input
              [:input.small {:id "width" :name "width"
                             :size 10 :type "text" :value (:width graph)}]]]
            [:div.clearfix
             [:label {:for "height"} "Height (in)"]
             [:div.input
              [:input.small {:id "height" :name "height"
                             :size 10 :type "text" :value (:height graph)}]]]
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
              {:name "ftype" :value "svg" :type "submit"}]]]))

(defpartial show-graph
  [run graph comparative-fields control-fields & opts]
  (let [widthpx (int (* 100 (:width graph)))
        heightpx (int (* 100 (:height graph)))]
    [:div
     [:div.row
      [:div.span12.columns
       [:a {:name (if (:templateid graph)
                    (format "template%d" (:templateid graph))
                    (format "graph%d" (:graphid graph)))}
        [:h3 (format "%s%s" (:name graph)
                (if (:templateid graph) " (template)" ""))]]
       [:p (:caption graph)]]]
     [:div.row
      [:div.span12.columns
       [:p
        (if-let [err (:err (render-graph-file run graph "png" "website"
                                              (:width graph) (:height graph)))]
          [:div
           [:pre err]
           (if (:templateid graph)
             [:div
              [:p
               [:a.code_header "Code"] " / "
               [:a.update_header "Update"]]
              [:pre.code {:style (format "width: %dpx;" widthpx)} (:code graph)]
              [:div]
              [:div.update (template-graph-form
                            run graph comparative-fields control-fields)]]
             [:p
              [:a.code_header "Code"] " / "
              (link-to (format "/graphs/update/%s" (:graphid graph)) "Update")])
           [:pre.code {:style (format "width: %dpx;" widthpx)} (:code graph)]]        
          [:div
           [:img {:src (if (:templateid graph)
                         (format "/graph/template/%s/%s/png" (:runid run) (:templateid graph))
                         (format "/graph/%s/%s/png" (:runid run) (:graphid graph)))
                  :width widthpx
                  :height heightpx}]
           (if (not (some #{:no-select} opts))
             (if (:templateid graph)
               ;; show template-graph links/form
               [:div
                [:p
                 [:a.code_header "Code"] " / "
                 [:a.update_header "Update"] " / "
                 [:a.download_header "Download"]]
                [:pre.code {:style (format "width: %dpx;" widthpx)} (:code graph)]
                [:div.download (graph-download-form run graph)]
                [:div.update (template-graph-form
                              run graph comparative-fields control-fields)]]
               ;; otherwise, show graph links/form
               [:div
                [:p
                 [:a.code_header "Code"] " / "
                 (link-to (format "/graphs/update/%s" (:graphid graph)) "Update")
                 " / "
                 [:a.download_header "Download"]]
                [:pre.code {:style (format "width: %dpx;" widthpx)} (:code graph)]
                [:div.download (graph-download-form run graph)]]))])]]]]))

(defpartial graphs
  [run comparative-fields control-fields & opts]
  (let [avail-graphs (filter #(or (:comparison run)
                             (= "non-comparative" (:resultstype %)))
                        (get (list-graphs) (:problem run)))
        active-graphs (set/union (set (get-run-graphs (:runid run)))
                             (set (get-run-template-graphs (:runid run))))]
    (if (or (not-empty avail-graphs) (not (some #{:no-select} opts)))
      [:section#graphs
       [:div.page-header
        [:a {:name "graphs"}
         [:h2 "Graphs"]]]
       (if (empty? active-graphs)
         [:div.row
          [:div.span12.columns [:p "No graphs."]]]
         (for [g (sort-by :name active-graphs) :when g]
           (apply show-graph run g comparative-fields control-fields opts)))
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
          [:div
           [:div.row
            [:div.span4.columns
             [:p [:b [:a.new_template_graph_form_header "New template graph..."]]]]]
           [:div.new_template_graph_form
            [:div.row
             [:div.span12.columns
              (template-graph-form run {} comparative-fields control-fields)]]]]])])))

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
  [:post "/graphs/update-template-graph"] {:as graph}
  (cond (= "Update" (:action graph))
        (do
          (update-template-graph graph)
          (resp/redirect (format "/run/%s#template%s" (:runid graph) (:templateid graph))))
        (= "Delete" (:action graph))
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/graphs/delete-template-graph-confirm" (:templateid graph)
                                  "Are you sure you want to delete the graph?"))
        :else
        (resp/redirect (format "/run/%s" (:runid graph)))))

(defpage
  [:post "/graphs/delete-template-graph-confirm"] {:as confirm}
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-template-graph (:id confirm))
      (resp/redirect "/"))
    (resp/redirect "/")))

(defpage
  [:post "/graphs/new-template-graph"] {:as graph}
  (let [templateid (new-template-graph graph)]
    (resp/redirect (format "/run/%s#template%d" (:runid graph) templateid))))

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
                                                (Integer/parseInt graphid)
                                                nil)))

(defpage "/graph/template/:runid/:templateid/png"
  {runid :runid templateid :templateid}
  (resp/content-type "image/png" (get-graph-png (Integer/parseInt runid)
                                                nil
                                                (Integer/parseInt templateid))))

(defpage [:post "/graph/download"] {:as graph}
  (->
   (resp/content-type (cond (= "pdf" (:ftype graph))
                            "application/pdf"
                            (= "svg" (:ftype graph))
                            "image/svg+xml"
                            (= "png" (:ftype graph))
                            "image/png")
                      (get-graph-download (Integer/parseInt (:runid graph))
                                          (try
                                            (Integer/parseInt (:graphid graph))
                                            (catch Exception _ nil))
                                          (try
                                            (Integer/parseInt (:templateid graph))
                                            (catch Exception _ nil))
                                          (:ftype graph)
                                          (:theme graph)
                                          (Double/parseDouble (:width graph))
                                          (Double/parseDouble (:height graph))))
   (header "Content-Disposition"
           (format "attachment; filename=\"%s.%s\"" (:filename graph) (:ftype graph)))))
