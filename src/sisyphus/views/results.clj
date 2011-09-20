(ns sisyphus.views.results
  (:use noir.core hiccup.core hiccup.page-helpers hiccup.form-helpers))

(defpartial comparative-results-table
  [comparative-results on-fields]
  [:div.row
   [:div.span16.columns {:style "max-width: 960px; max-height: 30em; overflow: auto;"}
    [:table.tablesorter.zebra-striped
     [:thead
      [:tr (map (fn [f] [:th (name f)]) on-fields)]]
     [:tbody
      (map (fn [r] [:tr (map (fn [f] [:td (let [val (get r f)]
                                            (if (= java.lang.Double (type val))
                                              (format "%.2f" val)
                                              (str val)))])
                             on-fields)])
           comparative-results)]]]])

(defpartial paired-results-table
  [control-results comparison-results on-fields]
  [:div.row
   [:div.span16.columns {:style "max-width: 960px; max-height: 30em; overflow: auto;"}
    [:table.tablesorter.zebra-striped
     [:thead
      [:tr (map (fn [f] [:th (name f)]) on-fields)]]
     [:tbody
      (map (fn [i]
             [:tr (map (fn [f]
                         [:td (let [control-val (get (nth control-results i) f)
                                    comparison-val (get (nth comparison-results i) f)]
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
                       on-fields)])
           (range (min (count control-results) (count comparison-results))))]]]])

