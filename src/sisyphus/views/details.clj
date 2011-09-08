(ns sisyphus.views.details
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.runs :only [get-doc get-results]]))

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
   [:div.span4.columns
    [:h2 "Annotations"]]
   [:div.span8.columns [:p "No annotations."]]]
  [:div.row
   [:div.span4.columns
    [:h3 "New annotation"]]
   [:div.span12.columns
    [:p (text-area :annotation)]
    [:p (submit-button "Save")]]])

(defpartial details-table
  [run]
  (let [comparative-results
        (map (fn [r] (dissoc r :Problem :Comparison :Control :runid :type :_rev :_id))
             (map :value (:rows (get-results run :comparative))))
        fields (sort (apply set/intersection (map (fn [r] (set (keys r))) comparative-results)))]
    [:div
     [:h2 "Results"]
     [:table.tablesorter.zebra-striped
      [:thead
       [:tr
        (map (fn [f] [:th (name f)]) fields)]]
      [:tbody
       (map (fn [r] [:tr (map (fn [f]
                                [:td
                                 (let [val (get r f)]
                                   (if (= java.lang.Double (type val))
                                     (format "%.2f" val)
                                     (str val)))])
                              fields)])
            comparative-results)]]
     [:div.row
      [:div.span4.columns [:h3 "Fields"]]
      (let [field-groups (partition-all (int (Math/ceil (/ (count fields) 3))) fields)]
        (map (fn [fs]
               [:div.span4.columns
                [:div
                 [:ul.inputs-list
                  (map (fn [f]
                         [:li [:label (check-box :fields false (name f)) " " (name f)]])
                       fs)]]])
             field-groups))]]))

(defpage "/details/:id" {id :id}
  (let [doc (get-doc id)]
    (if (= "run" (:type doc))
      (common/layout
       [:h1 (format "%s Run" (:problem doc))]
       (details-table doc)
       (details-annotations doc)
       (details-metainfo doc))
      (common/layout
       [:h1 "blah"]))))
