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
          [:h4 "Parameters"]
          [:pre (str params)]]
         [:div
          [:h4 "Control parameters"]
          [:pre (str (first params))]
          [:h4 "Comparison parameters"]
          [:pre (str (second params))]])]]]))

;; A results table with single rows (not paired rows); used for
;; comparative results or non-comparative runs (i.e. control results)
(defpartial results-table
  [results on-fields]
  [:div.row
   [:div.span16.columns {:style "max-width: 960px; max-height: 20em; overflow: auto;"}
    [:table.tablesorter.zebra-striped
     [:thead
      [:tr [:th "Params"] (map (fn [f] [:th (name f)]) on-fields)]]
     [:tbody
      (for [i (range (count results))]
        (let [r (nth results i)]
          [:tr [:td (if (and (:control-params r) (:comparison-params r))
                      (params-modal i :comparative
                                    [(:control-params r) (:comparison-params r)])
                      (params-modal i :control (:control-params r)))]
           (map (fn [f] [:td (let [val (get r f)]
                               (if (= java.lang.Double (type val))
                                 (format "%.2f" val)
                                 (str val)))])
                on-fields)]))]]]])

;; Group results together for a paired table
(defn build-results-map
  [control-results comparison-results]
  (letfn [(make-key [r] [(:Seed r) (:control-params r) (:comparison-params r)])]
    (reduce (fn [m r] (update-in m [(make-key r)] conj r))
            (zipmap (map make-key control-results)
                    (map (fn [r] [r]) control-results))
            comparison-results)))

;; A paired results table; each cell has either one or two values: one
;; if control/comparison values are identical, otherwise two values,
;; the top bold representing comparison value, the bottom regular font
;; representing control value
(defpartial paired-results-table
  [paired-results on-fields]
  [:div.row
   [:div.span16.columns {:style "max-width: 960px; max-height: 20em; overflow: auto;"}
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
                           (if (and (= java.lang.Double (type control-val))
                                    (= java.lang.Double (type comparison-val)))
                             (format "<strong>%.2f</strong><br/>%.2f"
                                     comparison-val control-val)
                             (format "<strong>%s</strong><br/>%s"
                                     (str comparison-val) (str control-val)))
                           (if (= java.lang.Double (type control-val))
                             (format "%.2f" control-val)
                             (str control-val))))])
                on-fields)]))]]]])
