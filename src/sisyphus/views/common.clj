(ns sisyphus.views.common
  (:require [clojure.string :as str])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers)
  (:import [com.petebevin.markdown MarkdownProcessor]))

(defpartial layout
  [title & content]
  (html5
   [:head
    [:title (format "%s | Sisyphus" title)]
    (include-css "/css/bootstrap-1.3.0.min.css")
    (include-css "/css/tablesorter/style.css")
    (include-css "/css/sisyphus.css")
    (include-js "/js/jquery-1.6.3.min.js")
    (include-js "/js/jquery.tablesorter.min.js")
    (include-js "/js/bootstrap-modal.js")
    (include-js "/js/sisyphus.js")
    (javascript-tag "$(document).ready(function()
                     { $(\"table.tablesorter\").each(function(index)
                       { $(this).tablesorter(); }) });")]
   [:body {:style "padding-top: 50px;"}
    [:div.container
     [:div.topbar
      [:div.topbar-inner
       [:div.container
        [:h3 (link-to "/" "Sisyphus")]
        [:ul.nav
         [:li (link-to "/" "Runs")]
         [:li (link-to "/claims" "Claims")]
         [:li (link-to "/parameters" "Parameters")]
         [:li (link-to "/graphs" "Graphs")]
         [:li (link-to "/analysis" "Analysis")]
         [:li (link-to "/configure" "Configure")]]]]]
     content]]))

(defpartial strategy-format
  [strategy]
  "")

(defpartial date-format
  [ms]
  (let [date (new java.util.Date (long ms))
        dateinstance (. java.text.DateFormat getDateTimeInstance
                        java.text.DateFormat/MEDIUM java.text.DateFormat/SHORT)]
    (. dateinstance format date)))

(def mdp (com.petebevin.markdown.MarkdownProcessor.))

(defpartial convert-md
  [s]
  (.markdown mdp s))

(defpartial confirm-deletion
  [post id msg]
  [:section#confirm
   [:div.page-header
    [:h2 "Confirm deletion"]]
   [:div.row
    [:div.span4.columns "&nbsp;"]
    [:div.span12.columns
     [:p msg]
     (form-to [:post post]
              (hidden-field :id id)
              [:div.actions
               [:input.btn.danger {:name "choice" :value "Confirm deletion" :type "submit"}]
               " "
               [:input.btn {:name "choice" :value "Cancel" :type "submit"}]])]]])
