(ns sisyphus.views.claims
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use sisyphus.models.common)
  (:use [sisyphus.models.runs :only [get-results get-fields]])
  (:use [sisyphus.models.claims :only
         [new-claim update-claim delete-claim list-claims
          add-claim-association remove-claim-association update-claim-association
          get-claim-association]])
  (:use [sisyphus.models.graphs :only [get-graph list-graphs]])
  (:use [sisyphus.views.graphs :only [show-graph]])
  (:use [sisyphus.views.results :only
         [field-checkboxes comparative-results-table paired-results-table]]))

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
    [:h1 (if (:title claim) (format "Claim: %s" (:title claim)) "New claim")]]
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
    (for [r (:runs claim)]
      [:div
       [:div.page-header
        [:a {:name (:runid r)}]
        [:h2 (:problem r) " run " (link-to (format "/details/%s" (:runid r))
                          (subs (:runid r) 22))]]
       [:div.row
        [:div.span4.columns
         [:h3 "Summary"]
         [:p (link-to (format "/claim/%s/update-association/%s"
                              (:_id claim) (:runid r))
                      "Update association")]]
        [:div.span12.columns
         [:p (:comment r)]]]
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
                       [:input.btn.danger {:value "Remove" :name "action" :type "submit"}]])]]])])]])

(defpartial claim-association-form
  [claim run claim-opts comparative-fields paired-fields]
  (let [association (if claim (get-claim-association claim run))]
    (form-to
     [:post (if claim "/claims/update-association" "/claims/add-association")]
     (hidden-field :claim (:_id claim))
     (hidden-field :runid (:_id run))
     (hidden-field :problem (:problem run))
     [:div.row
      [:div.span4.columns
       [:h2 (if claim "Update association" "New association")]]
      [:div.span12.columns
       (if-not claim
         [:div.clearfix
          [:label {:for "claim"} "Claim"]
          [:div.input
           (drop-down :claim claim-opts (:_id claim))]])
       [:div.clearfix
        [:label {:for "comment"} "Comment"]
        [:div.input
         [:textarea.xxlarge {:id "comment" :name "comment"} (:comment association)]
         [:span.help-block "Describe how this run provides support for or against the claim."]]]]]
     [:div.row
      [:div.span4.columns
       [:h3 "Comparative fields"]]
      (field-checkboxes run :comparative-fields comparative-fields)]
     [:div.row
      [:div.span4.columns
       [:h3 "Control/comparison fields"]]
      (field-checkboxes run :paired-fields paired-fields)]
     (let [graphs (get (list-graphs) (:problem run))
           graph-groups (partition-all (int (Math/ceil (/ (count graphs) 3))) graphs)]
       [:div.row
        [:div.span4.columns
         [:h3 "Graphs"]]
        (map (fn [gs]
               [:div.span4.columns
                [:ul.inputs-list
                 (map (fn [g]
                        [:li [:label [:input {:type "checkbox" :name "graphs[]" :value (:name g)
                                              :checked (if claim ((set (:graphs association)) (:name g)))}]
                              " " (:name g)]])
                      gs)]])
             graph-groups)])
     [:div.row
      [:div.span4.columns "&nbsp;"]
      [:div.span12.columns
       [:div.actions
        [:input.btn.primary {:value (if claim "Update" "Associate") :type "submit"}]]]])))

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
  [:post "/claims/add-association"] {:as association}
  (add-claim-association association)
  (resp/redirect (format "/details/%s#claims" (:runid association))))

(defpage
  [:post "/claims/update-association"] {:as association}
  (update-claim-association association)
  (resp/redirect (format "/claim/%s#%s" (:claim association) (:runid association))))

(defpage
  [:post "/claims/remove-association"] {:as association}
  (remove-claim-association association)
  (resp/redirect (format "/claim/%s" (:claim association))))

(defpage "/claim/:id/update-association/:runid" {id :id runid :runid}
  (let [claim (get-doc id)
        run (get-doc runid)
        comparative-results (get-results runid :comparative)
        comparative-fields (get-fields comparative-results)
        [control-results comparison-results]
        (map (fn [results-type] (get-results runid results-type))
             [:control :comparison])
        paired-fields (get-fields (concat control-results comparison-results))]
    (common/layout
     (format "Update association for claim: %s" (:title claim))
     (claim-association-form claim run nil comparative-fields paired-fields))))

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
