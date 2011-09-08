(ns sisyphus.views.common
  (:use noir.core
        hiccup.core
        hiccup.page-helpers))

(defpartial layout [& content]
            (html5
              [:head
               [:title "sisyphus"]
               (include-css "http://twitter.github.com/bootstrap/assets/css/bootstrap-1.2.0.min.css")
               (include-css "css/tablesorter/style.css")
               (include-js "http://code.jquery.com/jquery-1.6.3.min.js")
               (include-js "http://autobahn.tablesorter.com/jquery.tablesorter.min.js")
               (javascript-tag "$(document).ready(function() { $(\"table.tablesorter\").each(function(index) { $(this).tablesorter(); }) });")]
              [:body {:style "padding-top: 50px;"}
               [:div.container
                [:div.topbar
                 [:div.topbar-inner
                  [:div.container
                   [:h3 (link-to "/" "Sisyphus")]
                   [:ul.nav
                    [:li (link-to "/" "Overview")]]]]]
                content]]))

(defpartial date-format
  [ms]
  (let [date (new java.util.Date (long ms))
        dateinstance (. java.text.DateFormat getDateTimeInstance
                        java.text.DateFormat/MEDIUM java.text.DateFormat/SHORT)]
    (. dateinstance format date)))
