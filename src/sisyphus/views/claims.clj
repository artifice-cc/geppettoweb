(ns sisyphus.views.claims
  (:require [sisyphus.views.common :as common])
  (:require [noir.response :as resp])
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers)
  (:use sisyphus.models.common)
  (:use [sisyphus.models.runs :only [get-summary-results get-summary-fields]])
  (:use [sisyphus.models.claims :only
         [new-claim update-claim delete-claim list-claims
          add-claim-association remove-claim-association update-claim-association
          get-claim-association]])
  (:use [sisyphus.models.graphs :only [get-graph list-graphs]])
  (:use [sisyphus.models.analysis :only [get-analysis list-analysis]])
  (:use [sisyphus.views.graphs :only [show-graph]])
  (:use [sisyphus.views.analysis :only [show-analysis]])
  (:use [sisyphus.views.results :only
         [results-table paired-results-table]]))

(defpartial field-checkbox
  [n fieldstype field on-fields]
  [:li [:label
        [:input {:type "checkbox" :name (format "%s[]" (name n)) :value (name field)
                 :checked (on-fields (name field))}]
        " " (name field)]])

(defpartial field-checkboxes
  [run n fieldstype fields]
  (let [field-groups (partition-all (int (Math/ceil (/ (count fields) 3))) fields)
        on-fields (set (get run (keyword (format "%s-fields" (name fieldstype)))))]
    (map (fn [fs]
           [:div.span4.columns
            [:ul.inputs-list (map (fn [f] (field-checkbox n fieldstype f on-fields)) fs)]])
         field-groups)))

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
             comparative? (= "comparative" (:paramstype run))
             comparative-results (get-summary-results (:runid r) :comparative)
             [control-results comparison-results]
             (map (fn [results-type] (get-summary-results (:runid r) results-type))
                  [:control :comparison])]
         [:div
          (if comparative?
            [:div.row
             [:div.span16.columns
              [:h4 "Comparative results"]
              (results-table comparative-results
                             (map keyword (:comparative-fields r)))]])
          (if comparative?
            [:div.row
             [:div.span16.columns
              [:h4 "Control/comparison results"]
              (paired-results-table control-results comparison-results
                                    (map keyword (:paired-fields r)))]])
          (if-not comparative?
            [:div.row
             [:div.span16.columns
              [:h4 "Results"]
              (results-table control-results (map keyword (:non-comparative-fields r)))]])
          (for [g (map (fn [n] (get-graph (:problem r) n)) (:graphs r))]
            (show-graph run g))
          (for [a (map (fn [n] (get-analysis (:problem r) n)) (:analysis r))]
            (show-analysis run a))
          [:div.row
           [:div.span4.columns "&nbsp;"]
           [:div.span12.columns
            (form-to [:post "/claims/remove-association"]
                     (hidden-field :claim (:_id claim))
                     (hidden-field :runid (:runid r))
                     [:div.actions
                      [:input.btn.danger {:value "Remove" :name "action" :type "submit"}]])]]])])]])

(defpartial claim-association-form-header
  [claim run claim-opts association]
  [:div
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
       [:span.help-block "Describe how this run provides support for or against the claim."]]]]]])

(defpartial claim-association-form-footer
  [claim run association]
  [:div
   (let [graphs (filter #(= (:paramstype run) (:resultstype %)) (get (list-graphs) (:problem run)))
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
   (let [analysis (filter #(= (:paramstype run) (:resultstype %)) (get (list-analysis) (:problem run)))
         analysis-groups (partition-all (int (Math/ceil (/ (count analysis) 3))) analysis)]
     [:div.row
      [:div.span4.columns
       [:h3 "Analysis"]]
      (map (fn [as]
             [:div.span4.columns
              [:ul.inputs-list
               (map (fn [a]
                      [:li [:label [:input {:type "checkbox" :name "analysis[]" :value (:name a)
                                            :checked (if claim ((set (:analysis association))
                                                                (:name a)))}]
                            " " (:name a)]])
                    as)]])
           analysis-groups)])
   [:div.row
    [:div.span4.columns "&nbsp;"]
    [:div.span12.columns
     [:div.actions
      [:input.btn.primary {:value (if claim "Update" "Associate") :type "submit"}]]]]])

(defpartial claim-association-form-comparative
  [claim run claim-opts comparative-fields paired-fields]
  (let [association (if claim (get-claim-association claim run))]
    (form-to
     [:post (if claim "/claims/update-association" "/claims/add-association")]
     (claim-association-form-header claim run claim-opts association)
     [:div.row
      [:div.span4.columns
       [:p [:b [:a.fields_checkboxes_header "Select comparative fields..."]]]]]
     [:div.fields_checkboxes
      [:div.row
       [:div.span4.columns "&nbsp;"]
       (field-checkboxes run :comparative-fields :comparative comparative-fields)]]
     [:div.row
      [:div.span4.columns
       [:p [:b [:a.fields_checkboxes_header "Select control / comparison fields..."]]]]]
     [:div.fields_checkboxes
      [:div.row
       [:div.span4.columns "&nbsp;"]
       (field-checkboxes run :paired-fields :paired paired-fields)]]
     (claim-association-form-footer claim run association))))

(defpartial claim-association-form-non-comparative
  [claim run claim-opts fields]
  (let [association (if claim (get-claim-association claim run))]
    (form-to
     [:post (if claim "/claims/update-association" "/claims/add-association")]
     (claim-association-form-header claim run claim-opts association)
     [:div.row
      [:div.span4.columns
       [:h3.fields_checkboxes_header "Select fields..."]]]
     [:div.fields_checkboxes
      [:div.row
       [:div.span4.columns "&nbsp;"]
       (field-checkboxes run :non-comparative-fields :non-comparative fields)]]
     (claim-association-form-footer claim run association))))

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
        comparative? (= "comparative" (:paramstype run))
        comparative-results (get-summary-results runid :comparative)
        comparative-fields (get-summary-fields comparative-results)
        [control-results comparison-results]
        (map (fn [results-type] (get-summary-results runid results-type))
             [:control :comparison])
        control-fields (get-summary-fields control-results)
        paired-fields (get-summary-fields (concat control-results comparison-results))]
    (common/layout
     (format "Update association for claim: %s" (:title claim))
     (if comparative?
       (claim-association-form-comparative claim run nil comparative-fields paired-fields)
       (claim-association-form-non-comparative claim run nil control-fields)))))

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
