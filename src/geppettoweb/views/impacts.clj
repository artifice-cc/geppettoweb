(ns geppettoweb.views.impacts
  (:require [geppettoweb.views.common :as common])
  (:require [ring.util.response :as resp])
  (:use compojure.core hiccup.def hiccup.element hiccup.form hiccup.util)
  (:use [geppetto.runs :only [get-run get-results]])
  (:use [geppettoweb.views.run :only
         [run-parameters run-metainfo run-delete-run]])
  (:require [geppetto.analysis]))

(defhtml effects-table
  [effects]
  [:table.tablesorter.zebra-striped
   [:thead
    [:tr [:th "Metric"]
     (for [param (keys (first (vals effects)))]
       [:th param])]]
   [:tbody
    (for [[metric effs] (sort-by (comp name first) effects)]
      [:tr [:td metric]
       (for [[param stats] (sort-by (comp name first) effs)]
         [:td (format "%.7f" (:p-value stats))])])]])

(defn show-impacts
  [runid]
  (let [run (get-run runid)
        effects (geppetto.analysis/calc-effect (get-results runid :control nil))]
    (common/layout
     (format "%s/%s run %s parameter impacts" (:problem run) (:name run) runid)
     [:div.header.jumbotron.subhead
      [:div.row-fluid
       [:h1 (format "%s/%s run %s parameter impacts <small>(%s)</small>"
                    (:problem run) (:name run)
                    (format "<a href=\"/run/%s\">%s</a>" runid runid)
                    (if (:comparison run)
                      "comparative" "non-comparative"))]]]
     (effects-table effects)
     (run-parameters run)
     (run-metainfo run)
     (run-delete-run run))))

(defroutes impacts-routes
  (context "/run/impacts" []
           (GET "/:runid" [runid]
                (show-impacts runid))))

