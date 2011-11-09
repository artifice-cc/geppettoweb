(ns sisyphus.views.annotations
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(defpartial annotations
  [doc type]
  [:section#annotations
   [:div.page-header
    [:a {:name "annotations"}
     [:h2 "Annotations"]]]]
  [:div.row
   [:div.span4.columns "&nbsp;"]
   [:div.span8.columns
    (if (or (nil? (:annotations doc)) (empty? (:annotations doc)))
      [:p "No annotations."]
      (map (fn [i]
             (form-to [:post (format "/%s/delete-annotation" type)]
                      (hidden-field :id (:_id doc))
                      (hidden-field :index i)
                      [:blockquote [:p (nth (:annotations doc) i)]]
                      [:p {:style "text-align: right;"} (submit-button "Delete")]))
           (range (count (:annotations doc)))))]]
  [:div.row
   [:div.span4.columns
    [:h3 "New annotation"]]
   [:div.span12.columns
    (form-to
     [:post (format "/%s/add-annotation" type)]
     (hidden-field :id (:_id doc))
     [:div.clearfix
      [:label {:for "content"} "Content"]
      [:div.input
       [:textarea.xxlarge {:id "content" :name "content"}]]]
     [:div.actions
      [:input.btn.primary {:value "Save" :type "submit"}]])]])
