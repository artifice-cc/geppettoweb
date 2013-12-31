(ns geppettoweb.views.common
  (:require [clojure.string :as str])
  (:use hiccup.core hiccup.page hiccup.def hiccup.element hiccup.form hiccup.util)
  (:import [com.petebevin.markdown MarkdownProcessor]))

(defn layout
  [title & content]
  (let [chtml (html content)]
    (html5
     [:head
      [:title (format "%s | Geppetto" title)]
      (include-css "/css/bootstrap.cosmo.min.css")
      (include-css "/css/bootswatch.css")
      (include-css "/css/tablesorter/style.css")
      (include-css "/css/geppettoweb.css")
      (include-js "/js/jquery-1.6.3.min.js")
      (include-js "/js/jquery.tablesorter.min.js")
      (include-js "/js/bootstrap-modal.js")
      (include-js "/js/ace/ace.js")
      (include-js "/js/geppettoweb.js")]
     [:body
      [:a {:name "top"}]
      [:div.navbar
       [:div.navbar-inner
        [:div.container
         [:a.brand {:href "/"} "Geppetto"]
         [:div.nav-collapse
          [:ul.nav
           [:li (link-to "/" "Runs")]
           [:li (link-to "/parameters" "Parameters")]
           [:li (link-to "/graphs" "Graphs")]
           [:li (link-to "/analyses" "Analyses")]]]]]]
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

(def mdp (com.petebevin.markdown.MarkdownProcessor.))

(defhtml convert-md
  [f]
  (.markdown mdp (slurp f)))

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
