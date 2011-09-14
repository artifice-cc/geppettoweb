(ns sisyphus.views.details
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:require [noir.cookies :as cookies])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.runs :only
         [get-results get-fields add-annotation delete-annotation delete-run]])
  (:use [sisyphus.models.graphs :only [get-graph-png list-graphs]]))

(defpartial details-metainfo
  [run]
  [:section#metadata
   [:div.page-header
    [:h2 "Metadata"]]
   [:div.row
    [:div.span4.columns
     [:h3 "Source code"]
     [:p "Commit " (link-to (format "https://github.com/joshuaeckroth/retrospect/commit/%s" (:commit run))
                            (subs (:commit run) 0 10))]]
    [:div.span12.columns
     [:pre (:commit-msg run)]]]
   [:div.row
    [:div.span4.columns
     [:h3 "Parameters"]]
    [:div.span4.columns
     [:dl [:dt "Control strategy"]
      [:dd (common/strategy-format (:control-strategy run))]]
     [:dl [:dt "Comparison strategy"]
      [:dd (common/strategy-format (:comparison-strategy run))]]]
    [:div.span4.columns
     [:dl [:dt "Reptitions"]
      [:dd (:repetitions run)]]
     [:dl [:dt "Seed"]
      [:dd (:seed run)]]]]
   [:div.row
    [:div.span4.columns
     [:h3 "Machine"]]
    [:div.span4.columns
     [:dl [:dt "Hostname"]
      [:dd (:hostname run)]]
     [:dl [:dt "Number of threads"]
      [:dd (:nthreads run)]]]
    [:div.span4.columns
     [:dl [:dt "Working directory"]
      [:dd (:pwd run)]]]
    [:div.span4.columns
     [:dl [:dt "Data directory"]
      [:dd (:datadir run)]]
     [:dl [:dt "Records directory"]
      [:dd (:recordsdir run)]]]]])

(defpartial details-annotations
  [run]
  [:section#annotations
   [:div.page-header
    [:a {:name "annotations"}
     [:h2 "Annotations"]]]]
  [:div.row
   [:div.span4.columns "&nbsp;"]
   [:div.span8.columns
    (if (or (nil? (:annotations run)) (empty? (:annotations run)))
      [:p "No annotations."]
      (map (fn [i]
             (form-to [:post "/details/delete-annotation"]
                      (hidden-field :id (:_id run))
                      (hidden-field :index i)
                      [:blockquote [:p (nth (:annotations run) i)]]
                      [:p {:style "text-align: right;"} (submit-button "Delete")]))
           (range (count (:annotations run)))))]]
  [:div.row
   [:div.span4.columns
    [:h3 "New annotation"]]
   [:div.span12.columns
    (form-to
     [:post "/details/add-annotation"]
     (hidden-field :id (:_id run))
     [:p [:textarea {:class "xxlarge" :name "content"}]]
     [:p {:style "text-align: right;"} (submit-button "Save")])]])

(defn filter-on-fields
  [problem fields]
  (filter (fn [f] (= "true" (cookies/get (format "%s-%s" problem (name f))))) fields))

(defpartial details-fields-checkboxes
  [run fields comparative?]
  (form-to
   [:post "/details/set-fields"]
   (hidden-field :id (:_id run))
   (hidden-field :comparative (if comparative? "true" "false"))
   (hidden-field :problem (:problem run))
   [:div.row
    (let [field-groups (partition-all (int (Math/ceil (/ (count fields) 4))) fields)]
      (map (fn [fs]
             [:div.span4.columns
              [:div
               [:ul.inputs-list
                (map (fn [f]
                       [:li [:label
                             [:input {:type "checkbox" :name "fields[]" :value (name f)
                                      :checked (= "true" (cookies/get
                                                          (format "%s-%s" (:problem run)
                                                                  (name f))))}]
                             " " (name f)]])
                     fs)]]])
           field-groups))]
   [:div.row [:div.span16.columns {:style "text-align: right"} (submit-button "Updated fields")]]))

(defpartial details-comparative-table
  [run]
  (let [comparative-results (get-results (:_id run) :comparative)
        fields (get-fields comparative-results)
        on-fields (filter-on-fields (:problem run) fields)]
    [:section#comparative
     [:div.page-header
      [:a {:name "comparative-results"}
       [:h2 "Comparative results"]]]
     [:div.row
      [:div.span16.columns {:style "max-width: 960px; max-height: 30em; overflow: auto;"}
       [:table.tablesorter.zebra-striped
        [:thead
         [:tr (map (fn [f] [:th (name f)]) on-fields)]]
        [:tbody
         (map (fn [r] [:tr (map (fn [f] [:td (let [val (get r f)]
                                               (if (= java.lang.Double (type val))
                                                 (format "%.2f" val)
                                                 (str val)))])
                                on-fields)])
              comparative-results)]]]]
     (details-fields-checkboxes run fields true)]))

(defpartial details-paired-table
  [run]
  (let [[control-results comparison-results]
        (map (fn [results-type] (get-results (:_id run) results-type))
             [:control :comparison])
        fields (get-fields (concat control-results comparison-results))
        on-fields (filter-on-fields (:problem run) fields)]
    [:section#comparison
     [:div.page-header
      [:a {:name "control-comparison-results"}
       [:h2 "Control/comparison results"]]]
     [:div.row
      [:div.span16.columns {:style "max-width: 960px; max-height: 30em; overflow: auto;"}
       [:table.tablesorter.zebra-striped
        [:thead
         [:tr (map (fn [f] [:th (name f)]) on-fields)]]
        [:tbody
         (map (fn [i]
                [:tr (map (fn [f]
                            [:td (let [control-val (get (nth control-results i) f)
                                       comparison-val (get (nth comparison-results i) f)]
                                   (if (not= control-val comparison-val)
                                     (if (and (= java.lang.Double (type control-val))
                                              (= java.lang.Double (type comparison-val)))
                                       (format "<strong>%.2f</strong><br/>%.2f"
                                               comparison-val control-val)
                                       (format "<strong>%s</strong><br/>%s"
                                               (str comparison-val) (str control-val)))
                                     (if (= java.lang.Double (type control-val))
                                       (format "%.2f" control-val)
                                       (str control-val))))])
                          on-fields)])
              (range (min (count control-results) (count comparison-results))))]]]]
     (details-fields-checkboxes run fields false)]))

(defpartial details-graphs
  [run]
  (let [graphs (get (list-graphs) (:problem run))]
    [:section#graphs
     [:div.page-header
      [:h2 "Graphs"]]
     (if (empty? graphs)
       [:div.row
        [:div.span16.columns [:p "No graphs."]]]
       (for [g graphs]
         (if-let [png (get-graph-png run g)]
           [:div.row
            [:div.span4.columns
             [:h3 (:name g) [:small (format " (%s)" (:results-type g))]]
             [:p (:caption g)]]
            [:div.span8.columns
             [:p
              [:img {:src png :width 700 :height 400}]]
             [:pre {:style "width: 650px;"} (:code g)]]]
           [:div.row
            [:div.span16.columns [:p (format "Failed to produce graph %s" (:name g))]]])))]))

(defpartial details-delete-run
  [run]
  [:section#delete
   [:div.page-header
    [:h2 "Delete"]]
   [:div.row
    [:div.span4.columns "&nbsp;"]
    [:div.span12.columns
     [:p "Delete run and all associated results?"]
     (form-to [:post "/details/delete-run"]
              (hidden-field :id (:_id run))
              [:div.actions
               [:input.btn.danger {:value "Delete run" :type "submit"}]])]]])

(defpage
  [:post "/details/delete-annotation"] {:as annotation}
  (delete-annotation (:id annotation) (Integer/parseInt (:index annotation)))
  (resp/redirect (format "/details/%s#annotations" (:id annotation))))

(defpage
  [:post "/details/add-annotation"] {:as annotation}
  (add-annotation (:id annotation) (:content annotation))
  (resp/redirect (format "/details/%s#annotations" (:id annotation))))

(defpage
  [:post "/details/delete-run"] {:as run}
  (common/layout
   "Confirm deletion"
   [:section#confirm
    [:div.page-header
     [:h2 "Confirm deletion"]]
    [:div.row
     [:div.span4.columns "&nbsp;"]
     [:div.span12.columns
      (form-to [:post "/details/delete-run-confirm"]
               (hidden-field :id (:id run))
               [:div.actions
                [:input.btn.danger {:name "choice" :value "Confirm deletion" :type "submit"}]
                " "
                [:input.btn {:name "choice" :value "Cancel" :type "submit"}]])]]]))

(defpage
  [:post "/details/delete-run-confirm"] {:as confirm}
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-run (:id confirm))
      (resp/redirect "/"))
    (resp/redirect (format "/details/%s" (:id confirm)))))

(defpage
  [:post "/details/set-fields"] {:as fields}
  (let [results (if (= "true" (:comparative fields))
                  (get-results (:id fields) :comparative)
                  (concat (get-results (:id fields) :control)
                          (get-results (:id fields) :comparison)))
        all-fields (get-fields results)
        on-fields (set (:fields fields))
        off-fields (set/difference (set (map name all-fields)) on-fields)]
    (doseq [f on-fields]
      (cookies/put! (keyword (format "%s-%s" (:problem fields) f)) "true"))
    (doseq [f off-fields]
      (cookies/put! (keyword (format "%s-%s" (:problem fields) f)) "false")))
  (resp/redirect (format "/details/%s#%s" (:id fields)
                         (if (= "true" (:comparative fields)) "comparative-results"
                             "control-comparison-results"))))

(defpage "/details/:id" {id :id}
  (let [doc (get-doc id)]
    (if (= "run" (:type doc))
      (common/layout (format "%s run %s" (:problem doc) (subs id 22))
       [:div.row [:div.span16.columns
                  [:h1 (format "%s run %s <small>(%s)</small>"
                               (:problem doc) (subs id 22)
                               (common/date-format (:time doc)))]]]
       (details-comparative-table doc)
       (details-paired-table doc)
       (details-graphs doc)
       (details-annotations doc)
       (details-metainfo doc)
       (details-delete-run doc))
      (common/layout "Blah"
       [:h1 "blah"]))))
