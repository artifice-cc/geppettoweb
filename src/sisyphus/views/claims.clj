(ns sisyphus.views.claims
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use [sisyphus.models.claims :only [new-claim list-claims]]))

(defpartial claim-form
  [claim]
  [:section#claim-form
   [:div.page-header
    [:h1 (if (:name claim) (format "Update claim %s" (:name claim))
             "New claim")]]
   (form-to
    [:post (if (:name claim) "/claims/update-claim" "/claims/new-claim")]
    (hidden-field :id (:_id claim))
    [:div.row
     [:div.span4.columns
      [:h2 "Claim information"]]
     [:div.span12.columns
      [:fieldset
       [:legend "Metadata"]
       [:div.clearfix
        [:label {:for "title"} "Title"]
        [:div.input
         [:input.xlarge {:id "title" :name "title" :size 30
                         :type "text" :value (:title claim)}]]]
       [:div.clearfix
        [:label {:for "description"} "Description"]
        [:div.input
         [:textarea.xxlarge {:id "description" :name "description"}
          (:description claim)]]]]
      [:fieldset
       [:legend "Verification"]
       [:div.clearfix
        [:label {:for "verification"} "Verification status"]
        [:div.input
         (drop-down :verification ["Unverified" "Verified true" "Verified false"]
                    (:verification claim))]]
       [:div.clearfix
        [:label {:for "conclusion"} "Conclusion"]
        [:div.input
         [:textarea.xxlarge {:id "conclusion" :name "conclusion"}
          (:conclusion claim)]]]]]]
    [:div.row
     [:div.span4.columns
      [:h2 "Runs"]]
     [:div.span12.columns
      "&nbsp;"]]
    [:div.row
     [:div.span4.columns "&nbsp;"]
     [:div.span12.columns
      [:div.actions
       [:input.btn.primary {:value "Save" :type "submit"}]]]])])

(defpage
  [:post "/claims/new-claim"] {:as claim}
  (new-claim claim)
  (resp/redirect "/claims"))

(defpage "/claims" []
  (common/layout
   "Claims"
   [:pre (str (list-claims))]
   (claim-form {})))
