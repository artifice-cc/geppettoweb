(ns sisyphus.views.claims
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use sisyphus.models.common)
  (:use [sisyphus.models.runs :only [get-results]])
  (:use [sisyphus.models.claims :only
         [new-claim update-claim delete-claim list-claims remove-claim-association]])
  (:use [sisyphus.models.graphs :only [get-graph]])
  (:use [sisyphus.views.graphs :only [show-graph]])
  (:use [sisyphus.views.results :only [comparative-results-table paired-results-table]]))

(defpartial claim-summary
  [claim]
  [:div
   [:p [:strong (link-to (format "/claim/%s" (:_id claim)) (:title claim))]
    " &mdash; " (:description claim)]
   [:p [:strong (:verification claim)] " (" (count (:runs claim)) " runs)"
    (if (not= "" (:conclusion claim)) " &mdash; ")
    (:conclusion claim)]
   [:hr]])

(defpartial claim-form
  [claim]
  [:section#claim-form
   [:div.page-header
    [:h1 (if (:title claim) (format "Claim: %s" (:title claim))
             "New claim")]]
   (form-to
    [:post (if (:title claim) "/claims/update-claim" "/claims/new-claim")]
    (hidden-field :id (:_id claim))
    [:div.row
     [:div.span4.columns "&nbsp;"]
     [:div.span12.columns
      [:div.clearfix
       [:label {:for "title"} "Title"]
       [:div.input
        [:input.xlarge {:id "title" :name "title" :size 30
                        :type "text" :value (:title claim)}]]]
      [:div.clearfix
       [:label {:for "description"} "Description"]
       [:div.input
        [:textarea.xxlarge {:id "description" :name "description"}
         (:description claim)]]]
      [:div.clearfix
       [:label {:for "verification"} "Verification status"]
       [:div.input
        (drop-down :verification ["Unverified" "Verified true" "Verified false"]
                   (:verification claim))]]
      [:div.clearfix
       [:label {:for "conclusion"} "Conclusion"]
       [:div.input
        [:textarea.xxlarge {:id "conclusion" :name "conclusion"}
         (:conclusion claim)]]]]]
    [:div.row
     [:div.span4.columns "&nbsp;"]
     [:div.span12.columns
      [:div.actions
       [:input.btn.primary {:value (if (:title claim) "Update" "Save") :name "action" :type "submit"}]
       " "
       (if (:title claim) [:input.btn.danger {:value "Delete" :name "action" :type "submit"}])]]])])

(defpartial claim-details
  [claim]
  [:div
   (claim-form claim)
   [:section#runs   
    [:div.page-header
     [:h2 "Runs"]]
    (for [r (:runs claim)]
      [:div
       [:div.row
        [:div.span4.columns "&nbsp;"]
        [:div.span12.columns
         [:p [:strong (:problem r) " run " (link-to (format "/details/%s" (:runid r))
                                                    (subs (:runid r) 22))]
          " &mdash; " (:comment r)]]]
       (let [run (get-doc (:runid r))
             comparative-results (get-results (:runid r) :comparative)
             [control-results comparison-results]
             (map (fn [results-type] (get-results (:runid r) results-type))
                  [:control :comparison])]
         [:div
          [:div.row
           [:div.span16.columns
            [:h4 "Comparative results"]
            (comparative-results-table comparative-results
                                       (map keyword (:comparative-fields r)))]]
          [:div.row
           [:div.span16.columns
            [:h4 "Control/comparison results"]
            (paired-results-table control-results comparison-results
                                  (map keyword (:paired-fields r)))]]
          (for [g (map (fn [n] (get-graph (:problem r) n)) (:graphs r))]
            (show-graph run g))
          [:div.row
           [:div.span4.columns "&nbsp;"]
           [:div.span12.columns
            (form-to [:post "/claims/remove-association"]
                     (hidden-field :claim (:_id claim))
                     (hidden-field :runid (:runid r))
                     [:div.actions
                      [:input.btn.danger {:value "Remove association" :type "submit"}]])]]])])]])

(defpage
  [:post "/claims/new-claim"] {:as claim}
  (new-claim claim)
  (resp/redirect "/claims"))

(defpage
  [:post "/claims/update-claim"] {:as claim}
  (if (= "Update" (:action claim))
    (do
      (update-claim (dissoc claim :action))
      (resp/redirect (format "/claim/%s" (:id claim))))
    (do
      (delete-claim claim)
      (resp/redirect "/claims"))))

(defpage
  [:post "/claims/remove-association"] {:as association}
  (remove-claim-association association)
  (resp/redirect (format "/claim/%s" (:claim association))))

(defpage "/claim/:id" {id :id}
  (let [claim (get-doc id)]
    (common/layout
     (format "Claim: %s" (:title claim))
     (claim-details claim))))

(defpage "/claims" []
  (let [claims (list-claims)]
    (common/layout
     "Claims"
     [:section#claims
      [:div.page-header
       [:h1 "Unverified claims"]]
      (for [c (:unverified claims)]
        (claim-summary c))]
     [:div.page-header
      [:h1 "Verified claims"]]
     (for [c (:verified claims)]
       (claim-summary c))
     (claim-form {}))))
