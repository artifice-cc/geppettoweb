(ns sisyphus.views.details
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:require [noir.cookies :as cookies])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.common :only [get-doc]])
  (:use [sisyphus.models.runs :only
         [get-results get-fields add-annotation delete-annotation delete-run]])
  (:use [sisyphus.models.graphs :only [get-graph-png list-graphs]])
  (:use [sisyphus.models.claims :only [claim-select-options add-claim-association list-claims]])
  (:use [sisyphus.views.claims :only [claim-summary]])
  (:use [sisyphus.views.results :only [comparative-results-table paired-results-table]]))

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
     [:dl [:dt "Record directory"]
      [:dd (:recorddir run)]]]]])

(defpartial field-checkbox
  [run n field]
  [:li [:label
        [:input {:type "checkbox" :name (format "%s[]" (name n)) :value (name field)
                 :checked (= "true" (cookies/get (format "%s-%s" (:problem run) (name field))))}]
        " " (name field)]])

(defpartial field-checkboxes
  [run n fields]
  (let [field-groups (partition-all (int (Math/ceil (/ (count fields) 3))) fields)]
    (map (fn [fs]
           [:div.span4.columns
            [:ul.inputs-list (map (fn [f] (field-checkbox run n f)) fs)]])
         field-groups)))

(defpartial details-fields-form
  [run fields comparative?]
  (form-to
   [:post "/details/set-fields"]
   (hidden-field :id (:_id run))
   (hidden-field :comparative (if comparative? "true" "false"))
   (hidden-field :problem (:problem run))
   [:div.row
    [:div.span4.columns [:h3 "Active fields"]]
    (field-checkboxes run :fields fields)]
   [:div.row
    [:div.span4.columns "&nbsp;"]
    [:div.span12.columns
     [:div.actions
      [:input.btn.primary {:value "Update" :type "submit"}]]]]))

(defn filter-on-fields
  [problem fields]
  (filter (fn [f] (= "true" (cookies/get (format "%s-%s" problem (name f))))) fields))

(defpartial details-comparative-results-table
  [run comparative-results comparative-fields]
  (let [on-fields (filter-on-fields (:problem run) comparative-fields)]
    [:section#comparative
     [:div.page-header
      [:a {:name "comparative-results"}
       [:h2 "Comparative results"]]]
     (comparative-results-table comparative-results on-fields)
     (details-fields-form run comparative-fields true)]))

(defpartial details-paired-results-table
  [run control-results comparison-results paired-fields]
  (let [on-fields (filter-on-fields (:problem run) paired-fields)]
    [:section#comparison
     [:div.page-header
      [:a {:name "control-comparison-results"}
       [:h2 "Control/comparison results"]]]
     (paired-results-table control-results comparison-results on-fields)
     (details-fields-form run paired-fields false)]))

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
     [:div.clearfix
      [:label {:for "content"} "Content"]
      [:div.input
       [:textarea.xxlarge {:id "content" :name "content"}]]]
     [:div.actions
      [:input.btn.primary {:value "Save" :type "submit"}]])]])

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

(defpartial details-claims
  [run comparative-fields paired-fields]
  (let [claim-opts (claim-select-options run)
        run-claims (list-claims run)]
    [:section#claims
     [:div.page-header
      [:a {:name "claims"}
       [:h2 "Claims"]]]
     [:div.row
      [:div.span4.columns
       [:h2 "Associated claims"]]
      [:div.span12.columns
       (if (and (empty? (:verified run-claims))
                (empty? (:unverified run-claims)))
         [:p "No claims."]
         [:div
          (if (not-empty (:unverified run-claims))
            [:h3 "Unverified"])
          (for [c (:unverified run-claims)]
            (claim-summary c))
          (if (not-empty (:verified run-claims))
            [:h3 "Verified"])
          (for [c (:verified run-claims)]
            (claim-summary c))])]]
     (when (not-empty claim-opts)
       (form-to
        [:post "/details/associate-claim"]
        (hidden-field :runid (:_id run))
        (hidden-field :problem (:problem run))
        [:div.row
         [:div.span4.columns
          [:h2 "New association"]]
         [:div.span12.columns
          [:div.clearfix
           [:label {:for "claim"} "Claim"]
           [:div.input
            (drop-down :claim claim-opts)]]
          [:div.clearfix
           [:label {:for "comment"} "Comment"]
           [:div.input
            [:textarea.xxlarge {:id "comment" :name "comment"}]
            [:span.help-block "Describe how this run provides support for or against the claim."]]]]]
        [:div.row
         [:div.span4.columns
          [:h3 "Comparative fields"]]
         (field-checkboxes run :comparative-fields comparative-fields)]
        [:div.row
         [:div.span4.columns
          [:h3 "Control/comparison fields"]]
         (field-checkboxes run :paired-fields paired-fields)]
        [:div.row
         [:div.span4.columns "&nbsp;"]
         [:div.span12.columns
          [:div.actions
           [:input.btn.primary {:value "Associate" :type "submit"}]]]]))]))

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
  [:post "/details/associate-claim"] {:as association}
  (add-claim-association association)
  (resp/redirect (format "/details/%s#claims" (:runid association))))

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
      (let [comparative-results (get-results (:_id doc) :comparative)
            comparative-fields (get-fields comparative-results)
            [control-results comparison-results]
            (map (fn [results-type] (get-results (:_id doc) results-type))
                 [:control :comparison])
            paired-fields (get-fields (concat control-results comparison-results))]
        (common/layout
         (format "%s run %s" (:problem doc) (subs id 22))
         [:div.row [:div.span16.columns
                    [:h1 (format "%s run %s <small>(%s)</small>"
                                 (:problem doc) (subs id 22)
                                 (common/date-format (:time doc)))]]]
         (details-comparative-results-table doc comparative-results comparative-fields)
         (details-paired-results-table doc control-results comparison-results paired-fields)
         (details-graphs doc)
         (details-annotations doc)
         (details-claims doc comparative-fields paired-fields)
         (details-metainfo doc)
         (details-delete-run doc)))
      (common/layout "Blah"
                     [:h1 "blah"]))))
