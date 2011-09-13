(ns sisyphus.views.common
  (:require [clojure.string :as str])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers))

(defpartial layout
  [title & content]
  (html5
   [:head
    [:title (format "%s | Sisyphus" title)]
    (include-css "/css/bootstrap-1.2.0.min.css")
    (include-css "/css/tablesorter/style.css")
    (include-js "/js/jquery-1.6.3.min.js")
    (include-js "/js/jquery.tablesorter.min.js")
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
         [:li (link-to "/" "Overview")]
         [:li (link-to "/graphs" "Graphs")]]]]]
     content]]))

(defpartial strategy-format
  [strategy]
  (let [features (str/split strategy #",")]
    (interpose "," (for [f features] (if (= \! (nth f 0)) f [:strong f])))))

(defpartial date-format
  [ms]
  (let [date (new java.util.Date (long ms))
        dateinstance (. java.text.DateFormat getDateTimeInstance
                        java.text.DateFormat/MEDIUM java.text.DateFormat/SHORT)]
    (. dateinstance format date)))
