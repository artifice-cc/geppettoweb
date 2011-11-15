(ns sisyphus.views.claims
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use sisyphus.models.common)
  (:use [sisyphus.models.runs :only
         [get-summary-results get-summary-fields get-fields-funcs
          format-summary-fields]])
  (:use [sisyphus.models.claims :only
         [new-claim update-claim delete-claim list-claims
          add-claim-association remove-claim-association
          get-claim-association]])
  (:use [sisyphus.models.graphs :only [get-graph list-graphs]])
  (:use [sisyphus.models.analysis :only [get-analysis list-analysis]])
  (:use [sisyphus.views.fields :only [field-selects]])
  (:use [sisyphus.views.graphs :only [graphs]])
  (:use [sisyphus.views.analysis :only [analysis]])
  (:use [sisyphus.views.results :only
         [results-table paired-results-table]]))

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
       [:input.btn.primary
        {:value (if (:title claim) "Update" "Save") :name "action" :type "submit"}]
       " "
       (if (:title claim) [:input.btn.danger
                           {:value "Delete" :name "action" :type "submit"}])]]])])

(defpartial claim-details
  [claim]
  [:div
   (claim-form claim)
   [:section#runs
    (for [r (:runs claim)]
      (let [run (get-doc (:runid r))
            comparative? (= "comparative" (:paramstype run))]
        [:div
         [:div.page-header
          [:a {:name (:runid r)}]
          [:h2 (:problem run) " run " (link-to (format "/run/%s" (:runid r))
                                               (subs (:runid r) 22))]]
         [:div.row
          [:div.span4.columns
           [:h3 "Comment"]]
          [:div.span12.columns
           [:p (:comment r)]]]
         
         (if comparative?
           (let [fields-funcs (get-fields-funcs run :comparative)
                 on-fields (concat ["Simulation"] (format-summary-fields fields-funcs))
                 results (get-summary-results run :comparative fields-funcs)]
             [:div.row
              [:div.span16.columns
               [:h4 "Comparative results"]
               (results-table results on-fields)]]))
         (if comparative?
           (let [fields-funcs (get-fields-funcs run :paired)
                 on-fields (concat ["Simulation"] (format-summary-fields fields-funcs))
                 control-results (get-summary-results run :control fields-funcs)
                 comparison-results (get-summary-results run :comparison fields-funcs)]
             [:div.row
              [:div.span16.columns
               [:h4 "Control/comparison results"]
               (paired-results-table control-results comparison-results on-fields)]]))
         (if-not comparative?
           (let [fields-funcs (get-fields-funcs run :non-comparative)
                 on-fields (concat ["Simulation"] (format-summary-fields fields-funcs))
                 results (get-summary-results run :control fields-funcs)]
             [:div.row
              [:div.span16.columns
               [:h4 "Results"]
               (results-table results on-fields)]]))
         (analysis run :no-select)
         (graphs run :no-select)
         [:div.row
          [:div.span4.columns "&nbsp;"]
          [:div.span12.columns
           (form-to [:post "/claims/remove-association"]
                    (hidden-field :claim (:_id claim))
                    (hidden-field :runid (:runid r))
                    [:div.actions
                     [:input.btn.danger
                      {:value "Remove" :name "action" :type "submit"}]])]]]))]])

(defpage
  [:post "/claims/new-claim"] {:as claim}
  (new-claim claim)
  (resp/redirect "/claims"))

(defpage
  [:post "/claims/update-claim"] {:as claim}
  (cond (= "Update" (:action claim))
        (do
          (update-claim (dissoc claim :action))
          (resp/redirect "/claims"))
        (= "Delete" (:action claim))
        (common/layout
         "Confirm deletion"
         (common/confirm-deletion "/claims/delete-claim-confirm" (:id claim)
                                  "Are you sure you want to delete the claim?"))
        :else
        (resp/redirect "/claims")))

(defpage
  [:post "/claims/delete-claim-confirm"] {:as confirm}
  (if (= (:choice confirm) "Confirm deletion")
    (do
      (delete-claim (:id confirm))
      (resp/redirect "/claims"))
    (resp/redirect (format "/claim/%s" (:id confirm)))))

(defpage
  [:post "/claims/add-association"] {:as association}
  (add-claim-association association)
  (resp/redirect (format "/run/%s#claims" (:runid association))))

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
