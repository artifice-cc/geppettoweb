(ns sisyphus.views.results
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

;; Provide a modal dialog for the parameters for a single row in a
;; results table; the output of this defpartial should be placed in a
;; table cell
(defpartial params-modal
  [i resultstype params]
  (let [id (format "%s-%d" (name resultstype) i)]
    [:div.paramsmodal
     [:a {:href "#" :data-controls-modal id
          :data-keyboard "true" :data-backdrop "true"}
      "Params"]
     [:div.modal.hide.fade {:id id}
      [:div {:style "padding: 10px;"}
       (if (= resultstype :control)
         [:div
          [:h3 "Parameters"]
          [:pre (str (dissoc (read-string (or params "{}")) :simulation))]]
         [:div
          [:h3 "Control parameters"]
          [:pre (str (dissoc (read-string (or (first params) "{}")) :simulation))]
          [:h3 "Comparison parameters"]
          [:pre (str (dissoc (read-string (or (second params) "{}")) :simulation))]])]]]))

;; A results table with single rows (not paired rows); used for
;; comparative results or non-comparative runs (i.e. control results)
(defpartial results-table
  [results on-fields]
  [:div.row-fluid
   [:div.span12.columns {:style "max-width: 960px; max-height: 30em; overflow: auto;"}
    [:table.tablesorter.zebra-striped
     [:thead
      [:tr [:th "Params"] (map (fn [f] [:th (name f)]) on-fields)]]
     [:tbody
      (for [i (range (count results))]
        (let [r (nth results i)]
          [:tr [:td (if (and (:control-params r) (:comparison-params r))
                      (params-modal i :comparative
                                    [(:control-params r) (:comparison-params r)])
                      (params-modal i :control (:params r)))]
           (map (fn [f] [:td (let [val (get r f)]
                            (if (= java.lang.Float (type val))
                              (format "%.3f" val)
                              (try (format "%.3f" (Float/parseFloat val))
                                   (catch Exception _ (str val)))))])
              on-fields)]))]]]])

;; A paired results table; each cell has either one or two values: one
;; if control/comparison values are identical, otherwise two values,
;; the top bold representing comparison value, the bottom regular font
;; representing control value
(defpartial paired-results-table
  [control-results comparison-results on-fields]
  (let [paired-results (partition 2 (interleave control-results comparison-results))]
    [:div.row-fluid
     [:div.span12.columns {:style "max-width: 960px; max-height: 30em; overflow: auto;"}
      [:table.tablesorter.zebra-striped
       [:thead
        [:tr [:th "Params"] (map (fn [f] [:th (name f)]) on-fields)]]
       [:tbody
        (for [i (range (count paired-results))]
          (let [[control comparison] (nth paired-results i)]
            [:tr [:td (params-modal i :paired
                                    [(:control-params control)
                                     (:comparison-params comparison)])]
             (map (fn [f]
                  [:td (let [control-val (get control f)
                             comparison-val (get comparison f)]
                         (if (not= control-val comparison-val)
                           (if (and (= java.lang.Float (type control-val))
                                    (= java.lang.Float (type comparison-val)))
                             (format "<strong>%.3f</strong><br/>%.3f"
                                comparison-val control-val)
                             (format "<strong>%s</strong><br/>%s"
                                (str comparison-val) (str control-val)))
                           (if (= java.lang.Float (type control-val))
                             (format "%.3f" control-val)
                             (try (format "%.3f" (Float/parseFloat control-val))
                                  (catch Exception _ (str control-val))))))])
                on-fields)]))]]]]))
