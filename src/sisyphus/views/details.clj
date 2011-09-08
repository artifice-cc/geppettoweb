(ns sisyphus.views.details
  (:require [clojure.set :as set])
  (:require [sisyphus.views.common :as common])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.runs :only [get-doc get-results]]))

(def dissoc-fields [:Problem :Comparison :Control :Step :runid :type :_rev :_id])

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

(defpartial details-fields-checkboxes
  [fields comparative?]
  [:div
   [:div.row
    [:div.span16.columns [:h3 "Fields"]]]
   [:div.row
    (let [field-groups (partition-all (int (Math/ceil (/ (count fields) 4))) fields)]
      (map (fn [fs]
             [:div.span4.columns
              [:div
               [:ul.inputs-list
                (map (fn [f]
                       [:li [:label (check-box :fields false (name f)) " " (name f)]])
                     fs)]]])
           field-groups))]])

(defpartial details-comparative-table
  [run]
  (let [comparative-results (map (fn [r] (apply dissoc r dissoc-fields))
                                 (map :value (:rows (get-results run :comparative))))
        fields (sort (apply set/intersection (map (fn [r] (set (keys r))) comparative-results)))]
    [:div
     [:div.row
      [:div.span16.columns
       [:h2 "Comparative results"]]]
     [:div.row
      [:div.span16.columns {:style "max-width: 960px; overflow: auto;"}
       [:table.tablesorter.zebra-striped
        [:thead
         [:tr (map (fn [f] [:th (name f)]) fields)]]
        [:tbody
         (map (fn [r] [:tr (map (fn [f] [:td (let [val (get r f)]
                                               (if (= java.lang.Double (type val))
                                                 (format "%.2f" val)
                                                 (str val)))])
                                fields)])
              comparative-results)]]]]
     (details-fields-checkboxes fields true)]))

(defpartial details-paired-table
  [run]
  (let [[control-results comparison-results]
        (map (fn [results-type]
               (sort-by :Seed (map (fn [r] (apply dissoc r dissoc-fields))
                                   (map :value (:rows (get-results run results-type))))))
             [:control :comparison])
        fields (sort (apply set/intersection
                            (map (fn [r] (set (keys r)))
                                 (concat control-results comparison-results))))]
    [:div
     [:div.row
      [:div.span16.columns
       [:h2 "Control/comparison results"]]]
     [:div.row
      [:div.span16.columns {:style "max-width: 960px; overflow: auto;"}
       [:table.tablesorter.zebra-striped
        [:thead
         [:tr (map (fn [f] [:th (name f)]) fields)]]
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
                          fields)])
              (range (min (count control-results) (count comparison-results))))]]]]
     (details-fields-checkboxes fields false)]))

(defpage "/details/:id" {id :id}
  (let [doc (get-doc id)]
    (if (= "run" (:type doc))
      (common/layout
       [:div.row [:div.span16.columns
                  [:h1 (format "%s run %s (%s)"
                               (:problem doc) (subs id 0 8)
                               (common/date-format (:time doc)))]]]
       (details-comparative-table doc)
       (details-paired-table doc)
       (details-annotations doc)
       (details-metainfo doc))
      (common/layout
       [:h1 "blah"]))))
