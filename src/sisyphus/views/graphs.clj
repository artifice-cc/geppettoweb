(ns sisyphus.views.graphs
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use [ring.util.response :only [header]])
  (:require [clojure.string :as str])
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.graphs :only
         [list-graphs new-graph update-graph set-graphs delete-graph
          render-graph-file get-graph-png get-graph-download]])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(def graph-help (.markdown common/mdp (slurp "/home/josh/git/research/sisyphus/help/graphs.md")))

(defpartial show-graph
  [doc graph & opts]
  (let [params (get-doc (:paramsid doc) (:paramsrev doc))
        width (:width graph "7")
        height (:height graph "4")
        widthpx (* 100 (Integer/parseInt width))
        heightpx (* 100 (Integer/parseInt height))]
    [:div
     [:div.row
      [:div.span12.columns
       [:a {:name (str/replace (:name graph) #"\W" "_")}
        [:h3 (:name graph)]]
       [:p (:caption graph)]]]
     [:div.row
      [:div.span12.columns
       [:p
        (if-let [err (:err (render-graph-file doc graph "png" "website" width height))]
          [:div
           [:pre err]
           [:p
            [:a.code_header "Code"] " / "
            (link-to (format "/graphs/update/%s" (:_id graph)) "Update")]
           [:pre.code {:style (format "width: %dpx;" widthpx)} (:code graph)]]        
          [:div
           [:img {:src (format "/graph/%s/%s/%s/png" (:_id doc) (:_id graph) (:_rev graph))
                  :width widthpx
                  :height heightpx}]
           (if (not (some #{:no-select} opts))
             [:div
              [:p
               [:a.code_header "Code"] " / "
               (link-to (format "/graphs/update/%s" (:_id graph)) "Update")
               " / "
               [:a.download_header "Download"]]
              [:pre.code {:style (format "width: %dpx;" widthpx)} (:code graph)]
              [:div.download
               (form-to [:post "/graph/download"]
                        (hidden-field :docid (:_id doc))
                        (hidden-field :graphid (:_id graph))
                        (hidden-field :graphrev (:_rev graph))
                        [:fieldset
                         [:div.clearfix
                          [:label {:for "theme"} "Theme"]
                          [:div.input
                           (drop-down :theme ["paper" "poster" "website"])]]
                         [:div.clearfix
                          [:label {:for "width"} "Width (in)"]
                          [:div.input
                           [:input.xlarge {:id "width" :name "width"
                                           :size 3 :type "text" :value width}]]]
                         [:div.clearfix
                          [:label {:for "height"} "Height (in)"]
                          [:div.input
                           [:input.xlarge {:id "height" :name "height"
                                           :size 3 :type "text" :value (str height)}]]]
                         [:div.clearfix
                          [:label {:for "filename"} "File name (without extension)"]
                          [:div.input
                           [:input.xlarge
                            {:id "filename" :name "filename" :size 30 :type "text"
                             :value (format "%s-%s--%s--%s"
                                       (:problem doc) (:name params) (:name graph)
                                       (subs (:_id doc) 22))}]]]
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
              (hidden-field :id (:_id graph))
              [:fieldset
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
                [:label {:for "width"} "Width (inches)"]
                [:div.input
                 [:input.xlarge {:id "width" :name "width" :size 30
                                 :type "text" :value (:width graph "7")}]]]
               [:div.clearfix
                [:label {:for "height"} "Height (inches)"]
                [:div.input
                 [:input.xlarge {:id "height" :name "height" :size 30
                                 :type "text" :value (:height graph "4")}]]]
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
                 [:textarea.xxlarge {:id "code" :name "code" :rows 30
                                     :style "font-family: monospace;"}
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
                   {:value "Delete" :name "action" :type "submit"}])]])]]
   [:div.row
    [:div.span12.columns
     [:a {:name "help"}
      [:h1 "Help"]]]]
   [:div.row
    [:div.span12.columns graph-help]]])

(defpartial graphs
  [doc & opts]
  (let [all-graphs (filter #(and (or (= "comparative" (:paramstype doc))
                                     (= (:paramstype doc) (:resultstype %)))
                                 (= (:type doc) (:run-or-sim %)))
                           (get (list-graphs) (:problem doc)))
        run (if (= "run" (:type doc)) doc (get-doc (:runid doc)))
        active-graphs (set (map get-doc (get run (if (= "run" (:type doc))
                                                 :graphs :simulation-graphs))))]
    (if (or (not-empty active-graphs) (not (some #{:no-select} opts)))
      [:section#graphs
       [:div.page-header
        [:a {:name "graphs"}
         [:h2 "Graphs"]]]
       (if (empty? active-graphs)
         [:div.row
          [:div.span12.columns [:p "No graphs."]]]
         (for [g (sort-by :name active-graphs) :when g]
           (apply show-graph doc g opts)))
       (if-not (or (empty? all-graphs) (some #{:no-select} opts))
         [:div
          [:div.row
           [:div.span4.columns
            [:p [:b [:a.fields_checkboxes_header "Choose graphs..."]]]]]
          [:div.fields_checkboxes
           [:div.row
            [:div.span8.columns
             (form-to
              [:post "/graphs/set-graphs"]
              (hidden-field :docid (:_id doc))
              (hidden-field :runid (:_id run))
              (hidden-field :run-or-sim (:type doc))
              [:div.clearfix
               [:div.input
                [:ul.inputs-list
                 (for [g (sort-by :name all-graphs)]
                   [:li [:label
                         [:input {:type "checkbox" :name "graphs[]" :value (:_id g)
                                  :checked (active-graphs g)}]
                         " " (:name g)]])]]
               [:div.actions
                [:input.btn.primary {:value "Update" :type "submit"}]]])]]]])])))

(defpage
  [:post "/graphs/set-graphs"] {:as graphs}
  (set-graphs (:runid graphs) (:graphs graphs) (:run-or-sim graphs))
  (resp/redirect (format "/%s/%s#graphs" (:run-or-sim graphs) (:docid graphs))))

(defn get-graph-anchor
  [graph]
  (let [problems (str/split (:problem graph) #",")]
    (format "%s%s" (str/replace (first problems) #"\W" "_")
       (str/replace (:name graph) #"\W" "_"))))

(defpage
  [:post "/graphs/update-graph"] {:as graph}
  (cond (= "Update" (:action graph))
        (do
          (update-graph graph)
          (resp/redirect (format "/graphs#%s" (get-graph-anchor graph))))
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
  (resp/redirect (format "/graphs#%s" (get-graph-anchor graph))))

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
         [:div.span12.columns
          [:div.page-header
           [:a {:name (str/replace problem #"\W" "_")}
            [:h1 (format "%s graphs" problem)]]]]]
        (for [graph (sort-by :name (get graphs problem))]
          [:div.row
           [:div.span4.columns
            [:a {:name (get-graph-anchor graph)}
             [:h2 (:name graph) [:br]
              [:small (format "%s<br/>(%s by %s inches)<br/>(%s, %s)"
                         (:problem graph)
                         (:width graph "7") (:height graph "4")
                         (:run-or-sim graph) (:resultstype graph))]]]
            [:p (:caption graph)]
            [:p (link-to (format "/graphs/update/%s" (:_id graph))
                         "Update graph")]]
           [:div.span8.columns
            [:pre (:code graph)]]])])
     (graph-form {}))))

(defpage "/graph/:docid/:graphid/:graphrev/png"
  {docid :docid graphid :graphid graphrev :graphrev}
  (resp/content-type "image/png"
                     (get-graph-png (get-doc docid) (get-doc graphid graphrev))))

(defpage [:post "/graph/download"] {:as graph}
  (->
   (resp/content-type (cond (= "pdf" (:ftype graph))
                            "application/pdf"
                            (= "svg" (:ftype graph))
                            "image/svg+xml"
                            (= "png" (:ftype graph))
                            "image/png")
                      (get-graph-download (get-doc (:docid graph))
                                          (get-doc (:graphid graph) (:graphrev graph))
                                          (:theme graph) (:width graph) (:height graph)
                                          (:ftype graph)))
   (header "Content-Disposition"
           (format "attachment; filename=\"%s.%s\"" (:filename graph) (:ftype graph)))))


