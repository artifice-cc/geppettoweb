(ns geppettoweb.views.common
  (:require [clojure.string :as str])
  (:use hiccup.core hiccup.page hiccup.def hiccup.element hiccup.form hiccup.util)
  (:use geppettoweb.state)
  (:import [org.pegdown PegDownProcessor]))

(defn gurl [s] (str @app-context s))

(defn layout
  [title & content]
  (let [chtml (html content)]
    (html5
     [:head
      [:title (format "%s | %s" title @app-title)]
      (include-css (gurl "/css/bootstrap.cosmo.min.css"))
      (include-css (gurl "/css/bootswatch.css"))
      (include-css (gurl "/css/tablesorter/style.css"))
      (include-css (gurl "/css/geppettoweb.css"))
      (include-js (gurl "/js/jquery-1.6.3.min.js"))
      (include-js (gurl "/js/jquery.tablesorter.min.js"))
      (include-js (gurl "/js/bootstrap-modal.js"))
      (include-js (gurl "/js/ace/ace.js"))
      (include-js (gurl "/js/geppettoweb.js"))]
     [:body
      [:a {:name "top"}]
      [:div.navbar
       [:div.navbar-inner
        [:div.container
         [:a.brand {:href (gurl "/")} @app-title]
         [:div.nav-collapse
          [:ul.nav
           [:li (link-to (gurl "/") "Runs")]
           [:li (link-to (gurl "/parameters") "Parameters")]
           [:li (link-to (gurl "/graphs") "Graphs")]
           [:li (link-to (gurl "/analyses") "Analyses")]]]]]]
      [:div.container-fluid
       (let [headers (re-seq #"<a name=\"([^\"]+)\"><h(\d)>([^<]+)" chtml)]
         [:div.row-fluid
          [:div#geppetto-nav-column-container.span3
           [:div#geppetto-nav-column
            [:p
             (map (fn [[_ anchor ds title]]
                    (let [a (str/replace anchor #"\W" "_")
                          l (link-to (format "#%s" a) title)
                          d (Integer/parseInt ds)]
                      [:div (cond (= d 1) [:b l]
                                  (= d 2) [:i [:span "&nbsp;&nbsp;" l]]
                                  :else [:span "&nbsp;&nbsp;&nbsp;&nbsp;" l])])) headers)]]]
          [:div#geppetto-main-column.span11 chtml]])]])))

(defhtml date-format
  [timestamp]
  (let [date (new java.util.Date (.getTime timestamp))
        dateinstance (. java.text.DateFormat getDateTimeInstance
                        ;; date format, time format
                        java.text.DateFormat/SHORT java.text.DateFormat/SHORT)]
    (. dateinstance format date)))

(def pegdown (PegDownProcessor.))

(defhtml convert-md
  [f]
  (.markdownToHtml pegdown (slurp f)))

(defhtml confirm-deletion
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
