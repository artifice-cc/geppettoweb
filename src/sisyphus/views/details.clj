(ns sisyphus.views.details
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:require [noir.cookies :as cookies])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.runs :only
         [get-doc get-results get-fields add-annotation delete-annotation]])
  (:use [sisyphus.models.graphs :only [get-graph]]))

(defpartial details-metainfo
  [run]
  [:div.row
   [:div.span4.columns
    [:h2 "Metadata"]]
   [:div.span4.columns
    [:h3 "Source code"]
    [:dl [:dt "Commit hash"]
     [:dd (link-to (format "https://github.com/joshuaeckroth/retrospect/commit/%s" (:commit run))
                   (subs (:commit run) 0 8))]]
    [:dl [:dt "Commit message"]
     [:dd (:commit-msg run)]]]
   [:div.span4.columns
    [:h3 "Machine"]
    [:dl [:dt "Hostname"]
     [:dd (:hostname run)]]
    [:dl [:dt "Working directory"]
     [:dd (:pwd run)]]
    [:dl [:dt "Data directory"]
     [:dd (:datadir run)]]
    [:dl [:dt "Records directory"]
     [:dd (:recordsdir run)]]
    [:dl [:dt "Number of threads"]
     [:dd (:nthreads run)]]]
   [:div.span4.columns
    [:h3 "Parameters"]
    [:dl [:dt "Control strategy"]
     [:dd (:control-strategy run)]]
    [:dl [:dt "Comparison strategy"]
     [:dd (:comparison-strategy run)]]
    [:dl [:dt "Reptitions"]
     [:dd (:repetitions run)]]]])

(defpartial details-annotations
  [run]
  [:div.row
   [:div.span16.columns
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
    [:div
     [:div.row
      [:div.span16.columns
       [:a {:name "comparative-results"}
        [:h2 "Comparative results"]]]]
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
    [:div
     [:div.row
      [:div.span16.columns
       [:a {:name "control-comparison-results"}
        [:h2 "Control/comparison results"]]]]
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

(defpage
  [:post "/details/delete-annotation"] {:as annotation}
  (delete-annotation (:id annotation) (Integer/parseInt (:index annotation)))
  (resp/redirect (format "/details/%s#annotations" (:id annotation))))

(defpage
  [:post "/details/add-annotation"] {:as annotation}
  (add-annotation (:id annotation) (:content annotation))
  (resp/redirect (format "/details/%s#annotations" (:id annotation))))

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
      (common/layout (format "%s run %s" (:problem doc) (subs id 0 8))
       [:div.row [:div.span16.columns
                  [:h1 (format "%s run %s <small>(%s)</small>"
                               (:problem doc) (subs id 0 8)
                               (common/date-format (:time doc)))]]]
       (details-comparative-table doc)
       (details-paired-table doc)
       (details-annotations doc)
       (details-metainfo doc))
      (common/layout "Blah"
       [:h1 "blah"]))))
