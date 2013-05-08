(ns sisyphus.views.common
  (:require [clojure.string :as str])
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers)
  (:import [com.petebevin.markdown MarkdownProcessor]))

(defpartial layout
  [title & content]
  (let [chtml (html content)]
    (html5
     [:head
      [:title (format "%s | Sisyphus" title)]
      (include-css "/css/bootstrap.cosmo.min.css")
      (include-css "/css/bootswatch.css")
      (include-css "/css/tablesorter/style.css")
      (include-css "/css/sisyphus.css")
      (include-js "/js/jquery-1.6.3.min.js")
      (include-js "/js/jquery.tablesorter.min.js")
      (include-js "/js/bootstrap-modal.js")
      (include-js "/js/sisyphus.js")
      (javascript-tag "$(document).ready(function()
                     { $(\"table.tablesorter\").each(function(index)
                       { $(this).tablesorter(); }) });")]
     [:body
      [:a {:name "top"}]
      [:div.navbar
       [:div.navbar-inner
        [:div.container
         [:a.brand {:href "/"} "Sisyphus"]
         [:div.nav-collapse
          [:ul.nav
           [:li (link-to "/" "Runs")]
           [:li (link-to "/parameters" "Parameters")]
           [:li (link-to "/graphs" "Graphs")]
           [:li (link-to "/analyses" "Analyses")]]]]]]
      [:div.container-fluid
       (let [headers (re-seq #"<a name=\"([^\"]+)\"><h(\d)>([^<]+)" chtml)]
         [:div.row-fluid
          [:div.span3
           [:p
            (map (fn [[_ anchor ds title]]
                 (let [a (str/replace anchor #"\W" "_")
                       l (link-to (format "#%s" a) title)
                       d (Integer/parseInt ds)]
                   [:div (cond (= d 1) [:b l]
                               (= d 2) [:i [:span "&nbsp;&nbsp;" l]]
                               :else [:span "&nbsp;&nbsp;&nbsp;&nbsp;" l])])) headers)]]
          [:div.span9 chtml]])]])))

(defpartial date-format
  [timestamp]
  (let [date (new java.util.Date (.getTime timestamp))
        dateinstance (. java.text.DateFormat getDateTimeInstance
                        ;; date format, time format
                        java.text.DateFormat/SHORT java.text.DateFormat/SHORT)]
    (. dateinstance format date)))

(def mdp (com.petebevin.markdown.MarkdownProcessor.))

(defpartial convert-md
  [f]
  (.markdown mdp (slurp f)))

(defpartial confirm-deletion
  [post id msg]
  [:section#confirm
   [:div.page-header
    [:h1 "Confirm deletion"]]
   [:p msg]
   (form-to [:post post]
            (hidden-field :id id)
            [:div.form-actions
             [:input.btn.btn-danger {:name "choice" :value "Confirm deletion" :type "submit"}]
             " "
             [:input.btn {:name "choice" :value "Cancel" :type "submit"}]])])
