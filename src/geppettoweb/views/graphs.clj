(ns geppettoweb.views.graphs
  (:require [geppettoweb.views.common :as common])
  (:require [ring.util.response :as resp])
  (:require [clojure.string :as str])
  (:require [clojure.set :as set])
  (:use [geppettoweb.models.graphs :exclude [graphs]])
  (:use [geppettoweb.config])
  (:use compojure.core hiccup.def hiccup.element hiccup.form hiccup.util))

(defhtml graph-form
  [graph]
  [:section#graph-form
   [:div.page-header
    [:a {:name "new"}
     [:h1 (if (:name graph) (format "Update %s" (:name graph))
              "New graph")]]]
   [:form.form-horizontal {:method "POST" :action (if (:name graph) "/graphs/update-graph"
                                                      "/graphs/new-graph")}
    (hidden-field :graphid (:graphid graph))
    [:div.control-group
     [:label.control-label {:for "problems"} "Problems"]
     [:div.controls
      [:input {:id "problems" :name "problems" :size 30
               :type "text" :value (:problems graph)}]]]
    [:div.control-group
     [:label.control-label {:for "name"} "Name"]
     [:div.controls
      [:input {:id "name" :name "name" :size 30
               :type "text" :value (:name graph)}]]]
    [:div.control-group
     [:label.control-label {:for "width"} "Width"]
     [:div.controls
      [:div.input-append
       [:input.span6 {:id "width" :name "width" :size 5
                      :type "text" :value (:width graph)}]
       [:span.add-on "in"]]]]
    [:div.control-group
     [:label.control-label {:for "height"} "Height"]
     [:div.controls
      [:div.input-append
       [:input.span6 {:id "height" :name "height" :size 5
                      :type "text" :value (:height graph)}]
       [:span.add-on "in"]]]]
    [:div.control-group
     [:label.control-label {:for "resultstype"} "Results type"]
     [:div.controls
      (drop-down :resultstype ["non-comparative" "comparative"]
                 (:resultstype graph))]]
    [:div.control-group
     [:label.control-label {:for "caption"} "Caption"]
     [:div.controls
      [:textarea.input-xxlarge {:id "caption" :name "caption"} (:caption graph)]]]
    [:div.control-group
     [:label.control-label {:for "code"} "R code"]
     [:div.controls
      [:textarea.input-xxlarge {:id "code" :name "code" :rows 30
                                :style "font-family: monospace;"}
       (if (:code graph) (:code graph) "")]
      [:span.help-block "Assume the existence of a data table named
                                    'control', and tables 'comparison' and 'comparative'
                                    if the results type is comparative; also assume
                                    that that 'ggplot2' is loaded. Save the graph to
                                    the variable 'p'."]]]
    [:div.form-actions
     [:input.btn.btn-primary
      {:name "action" :value (if (:name graph) "Update" "Save")
       :type "submit"}]
     " "
     (if (:name graph)
       [:input.btn.btn-danger
        {:value "Delete" :name "action" :type "submit"}])]]
   [:a {:name "help"} [:h1 "Help"]]
   (common/convert-md "help/graphs.md")])

(defhtml template-graph-fields
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

(defhtml template-graph-form
  [run graph comparative-fields control-fields]
  [:form.form-horizontal {:method "POST" :action (if (:name graph) "/graphs/update-template-graph"
                                                     "/graphs/new-template-graph")}
   (hidden-field :runid (:runid run))
   (hidden-field :templateid (:templateid graph))
   [:div.control-group
    [:label.control-label {:for "template"} "Template"]
    [:div.controls
     (drop-down :template ["line" "line-comparative" "bars" "bars-comparative"
                           "points" "density" "histogram"]
                (:template graph))]]
   [:div.control-group
    [:label.control-label {:for "name"} "Name"]
    [:div.controls [:input.input-large {:id "name" :name "name" :size 30
                                         :type "text" :value (:name graph)}]]]
   [:div.control-group
    [:label.control-label {:for "caption"} "Caption"]
    [:div.controls
     [:textarea.input-xxlarge {:id "caption" :name "caption"} (:caption graph)]]]
   [:div.control-group
    [:label.control-label {:for "xfield"} "X field"]
    [:div.controls (template-graph-fields
                    graph :xfield comparative-fields control-fields)]]
   [:div.control-group
    [:label.control-label {:for "xlabel"} "X label"]
    [:div.controls [:input.input-large {:id "xlabel" :name "xlabel" :size 30
                                         :type "text" :value (:xlabel graph)}]]]
   [:div.control-group
    [:label.control-label {:for "yfield"} "Y field"]
    [:div.controls (template-graph-fields
                    graph :yfield comparative-fields control-fields)]]
   [:div.control-group
    [:label.control-label {:for "ylabel"} "Y label"]
    [:div.controls [:input.input-large {:id "ylabel" :name "ylabel" :size 30
                                         :type "text" :value (:ylabel graph)}]]]
   [:div.control-group
    [:label.control-label {:for "fill"} "Fill"]
    [:div.controls (template-graph-fields
                    graph :fill comparative-fields control-fields)]]
   [:div.control-group
    [:label.control-label {:for "color"} "Color"]
    [:div.controls (template-graph-fields
                    graph :color comparative-fields control-fields)]]
   [:div.control-group
    [:label.control-label {:for "linetype"} "Line type"]
    [:div.controls (template-graph-fields
                    graph :linetype comparative-fields control-fields)]]
   [:div.control-group
    [:label.control-label {:for "shape"} "Shape"]
    [:div.controls (template-graph-fields
                    graph :shape comparative-fields control-fields)]]
   [:div.control-group
    [:label.control-label {:for "facethoriz"} "Facet horizontal"]
    [:div.controls (template-graph-fields
                    graph :facethoriz comparative-fields control-fields)]]
   [:div.control-group
    [:label.control-label {:for "facetvert"} "Facet vertical"]
    [:div.controls (template-graph-fields
                    graph :facetvert comparative-fields control-fields)]]
   [:div.control-group
    [:label.control-label {:for "width"} "Width"]
    [:div.controls
     [:div.input-append
      [:input.span6 {:id "width" :name "width" :size 5
                     :type "text" :value (or (:width graph) "7.0")}]
      [:span.add-on "in"]]]]
   [:div.control-group
    [:label.control-label {:for "height"} "Height"]
    [:div.controls
     [:div.input-append
      [:input.span6 {:id "height" :name "height" :size 5
                     :type "text" :value (or (:height graph) "4.0")}]
      [:span.add-on "in"]]]]
   [:div.form-actions
    [:input.btn.btn-primary
     {:name "action" :value (if (:name graph) "Update" "Create")
      :type "submit"}]
    " "
    (if (:name graph)
      [:input.btn.btn-danger
       {:value "Delete" :name "action" :type "submit"}])]])

(defhtml graph-download-form
  [run graph]
  [:form.form-horizontal {:method "POST" :action "/graph/download"}
   (hidden-field :runid (:runid run))
   (hidden-field :graphid (:graphid graph))
   (hidden-field :templateid (:templateid graph))
   [:div.control-group
    [:label.control-label {:for "theme"} "Theme"]
    [:div.controls
     (drop-down :theme ["paper" "poster" "website"])]]
   [:div.control-group
    [:label.control-label {:for "width"} "Width"]
    [:div.controls
     [:div.input-append
      [:input.span6 {:id "width" :name "width" :size 5
                     :type "text" :value (:width graph)}]
      [:span.add-on "in"]]]]
   [:div.control-group
    [:label.control-label {:for "heigh"} "Height"]
    [:div.controls
     [:div.input-append
      [:input.span6 {:id "heigh" :name "height" :size 5
                     :type "text" :value (:height graph)}]
      [:span.add-on "in"]]]]
   [:div.control-group
    [:label.control-label {:for "filename"} "File name (without extension)"]
    [:div.controls
     [:input.input-xxlarge
      {:id "filename" :name "filename" :size 30 :type "text"
       :value (format "%s-%s-%d--%s"
                 (:problem run) (:name run) (:runid run)
                 (if (empty? (:name graph))
                   (format "template-%s" (:template graph))
                   (:name graph)))}]]]
   [:div.form-actions
    [:input.btn.btn-success
     {:name "ftype" :value "png" :type "submit"}]
    " "
    [:input.btn.btn-success
     {:name "ftype" :value "pdf" :type "submit"}]
    " "
    [:input.btn.btn-success
     {:name "ftype" :value "svg" :type "submit"}]]])

(defhtml show-graph
  [run graph comparative-fields control-fields & opts]
  (let [widthpx (int (* 100 (:width graph)))
        heightpx (int (* 100 (:height graph)))]
    [:div
     [:a {:name (if (:templateid graph)
                  (format "templategraph%d" (:templateid graph))
                  (format "graph%d" (:graphid graph)))}
      [:h2 (format "%s%s" (:name graph)
              (if (:templateid graph) (format " (template %s)" (:template graph)) ""))]]
     [:p (:caption graph)]
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
              [:pre.code (:code graph)]
              [:div.download (graph-download-form run graph)]
              [:div.update (template-graph-form run graph comparative-fields control-fields)]]
             ;; otherwise, show graph links/form
             [:div
              [:p
               [:a.code_header "Code"] " / "
               (link-to (format "/graphs/update/%s" (:graphid graph)) "Update")
               " / "
               [:a.download_header "Download"]]
              [:pre.code (:code graph)]
              [:div.download (graph-download-form run graph)]]))])]]))

(defhtml graphs
  [run comparative-fields control-fields & opts]
  (let [avail-graphs (filter #(or (:comparison run)
                             (= "non-comparative" (:resultstype %)))
                        (get (list-graphs) (:problem run)))
        active-graphs (set/union (set (get-run-graphs (:runid run)))
                             (set (get-run-template-graphs (:runid run))))]
    (if (or (not-empty avail-graphs) (not (some #{:no-select} opts)))
      [:section
       [:div.page-header
        [:a {:name "graphs"}
         [:h1 "Graphs"]]]
       (if (empty? active-graphs)
         [:p "No graphs."]
         (for [g (sort-by :name active-graphs) :when g]
           (apply show-graph run g comparative-fields control-fields opts)))
       (if-not (or (empty? avail-graphs) (some #{:no-select} opts))
         [:div
          [:div.row-fluid
           [:div.span12.columns
            [:p [:b [:a.fields_checkboxes_header "Choose graphs..."]]]]]
          [:div.fields_checkboxes
           (form-to
            [:post "/graphs/set-run-graphs"]
            (hidden-field :runid (:runid run))
            [:div.row-fluid
             (for [graph-group (partition-all (int (Math/ceil (/ (count avail-graphs) 2)))
                                              (sort-by :name avail-graphs))]
               [:div.span6
                (for [g graph-group]
                  [:label.checkbox
                   [:input {:type "checkbox" :name "graphids[]" :value (:graphid g)
                            :checked (active-graphs g)}]
                   " " (:name g)])])]
            [:div.form-actions
             [:input.btn.btn-primary {:value "Update" :type "submit"}]])]])
       [:div
        [:div.row-fluid
         [:div.span12.columns
          [:p [:b [:a.new_template_form_header "New template graph..."]]]]]
        [:div.new_template_form
         [:div.row-fluid
          [:div.span12.columns
           (template-graph-form run {} comparative-fields control-fields)]]]]])))

(defn update-graph-action
  [graphid action graph]
  (cond (= "Update" action)
        (do
          (update-graph graph)
          (resp/redirect (format "/graphs#graph%s" graphid)))
        (= "Delete" action)
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/graphs/delete-graph-confirm" graphid
                                  "Are you sure you want to delete the graph?"))
        :else
        (resp/redirect "/graphs")))

(defn delete-graph-confirm
  [id choice]
  (if (= choice "Confirm deletion")
    (do
      (delete-graph id)
      (resp/redirect "/graphs"))
    (resp/redirect "/graphs")))

(defn update-template-graph-action
  [runid templateid action graph]
  (cond (= "Update" action)
        (do
          (update-template-graph graph)
          (resp/redirect (format "/run/%s#templategraph%s" runid templateid)))
        (= "Delete" action)
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/graphs/delete-template-graph-confirm" templateid
                                  "Are you sure you want to delete the graph?"))
        :else
        (resp/redirect (format "/run/%s" runid))))

(defn delete-template-graph-confirm
  [id choice]
  (let [runid (get-run-for-template-graph id)]
    (if (= choice "Confirm deletion")
      (do
        (delete-template-graph id)
        (resp/redirect (format "/run/%d" runid)))
      (resp/redirect (format "/run/%d" runid)))))

(defn show-all-graphs []
  (let [graphs (list-graphs)]
    (common/layout
     "Graphs"
     (for [problem (sort (keys graphs))]
       [:section {:id problem}
        [:div.page-header
         [:a {:name (str/replace problem #"\W" "_")}
          [:h1 (format "%s graphs" problem)]]]
        (for [graph (sort-by :name (get graphs problem))]
          [:div.row-fluid
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

(defn download-graph
  [graph]
  (-> (resp/content-type (cond (= "pdf" (:ftype graph))
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
      (resp/header "Content-Disposition"
                   (format "attachment; filename=\"%s.%s\"" (:filename graph) (:ftype graph)))))

(defroutes graphs-routes
  (context "/graphs" []
    (POST "/set-run-graphs" [runid graphsid]
      (do (set-run-graphs runid graphsid)
          (resp/redirect (format "/run/%s#graphs" runid))))
    (POST "/update-graph" [graphid action :as {graph :params}]
      (update-graph-action graphid action graph))
    (POST "/delete-graph-confirm" [id choice]
      (delete-graph-confirm id choice))
    (POST "/new-graph" [:as {graph :params}]
      (let [graphid (new-graph graph)]
        (resp/redirect (format "/graphs#graph%d" graphid))))
    (POST "/update-template-graph" [runid templateid action :as {graph :params}]
      (update-template-graph-action runid templateid action graph))
    (POST "/delete-template-graph-confirm" [id choice]
      (delete-template-graph-confirm id choice))
    (POST "/new-template-graph" [:as {graph :params}]
      (let [templateid (new-template-graph graph)]
        (resp/redirect (format "/run/%s#templategraph%d" (:runid graph) templateid))))
    (GET "/update/:graphid" [graphid]
      (let [graph (get-graph graphid)]
        (common/layout
         (format "Update %s" (:name graph))
         (graph-form graph))))
    (GET "/" [] (show-all-graphs)))
  (context "/graph" []
    (GET "/:runid/:graphid/png" [runid graphid]
      (resp/content-type "image/png" (get-graph-png (Integer/parseInt runid)
                                                    (Integer/parseInt graphid)
                                                    nil)))
    (GET "/template/:runid/:templateid/png" [runid templateid]
      (resp/content-type "image/png" (get-graph-png (Integer/parseInt runid)
                                                    nil
                                                    (Integer/parseInt templateid))))
    (POST "/download" [:as {graph :params}]
      (download-graph graph))))
