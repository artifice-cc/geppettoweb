(ns sisyphus.models.runs
  (:require [clojure.set :as set])
  (:require [clojure.string :as str])
  (:require [com.ashafa.clutch :as clutch])
  (:use [sisyphus.models.claims :only [list-claims remove-claim-association]])
  (:use [sisyphus.models.simulations :only [get-simulation-fields]])
  (:use sisyphus.models.common))

(defn list-runs
  []
  (map :value (:rows (view "runs-list"))))

(defn delete-run
  [id]
  (let [run (get-doc id)
        claims (apply concat (vals (list-claims run)))]
    (doseq [c claims]
      (remove-claim-association {:claim (:_id c) :runid id}))
    (doseq [r (:results run)]
      (let [doc (get-doc r)]
        (delete-doc doc)))
    (delete-doc run)))

(defn list-projects
  []
  (sort (set (filter identity (map (comp :project :value)
                                   (:rows (view "runs-list")))))))

(defn set-project
  [id project]
  (let [doc (get-doc id)]
    (reset-doc-cache id)
    (clutch/with-db db
      (clutch/update-document doc {:project project}))))

(defn get-summary-fields
  [run results-type & opts]
  (get-simulation-fields (get-doc (first (:results run))) results-type))

(def funcs {:sum #(reduce + 0 %)
            :avg #(double (/ (reduce + 0 %) (count %)))
            :min #(apply min %)
            :max #(apply max %)
            :last last
            :first first})

(defn get-fields-funcs
  [run results-type & opts]
  (if (some #{:all} opts)
    ;; get all possible fields-funcs, not just those activated;
    ;; used by get-summary-results below for CSV output
    (mapcat (fn [field] (map (fn [func] [field (name func)]) (keys funcs)))
            (get-summary-fields run results-type))
    ;; get only activated fields-funcs
    (let [ffs (get run (keyword (format "%s-fields-funcs" (name results-type))))]
      (filter #(not= "N/A" (second %))
              (map (fn [field] [field (get ffs field)]) (keys ffs))))))

(defn set-fields-funcs
  [id fields results-type]
  (let [doc (get-doc id)]
    (reset-doc-cache id)
    (clutch/with-db db
      (clutch/update-document doc {(keyword (format "%s-fields-funcs" (name results-type)))
                                   fields}))))

(defn format-summary-fields
  [fields-funcs]
  (map (fn [[field func]] (format "%s (%s)" (name field) func)) fields-funcs))

(defn summarize-sim-results
  [sim results-type fields-funcs]
  (concat
   (if (:params (first (get sim results-type)))
     [(:params (first (get sim results-type)))]
     [(:control-params (first (get sim results-type)))
      (:comparison-params (first (get sim results-type)))])
   (for [[field func] fields-funcs]
     (let [vals (filter number? (map field (get sim results-type)))]
       (if (empty? vals) (get (first (get sim results-type)) field)
           (if-let [f (get funcs (keyword func))]
             (f vals)
             (last vals)))))))

(defn get-summary-results
  "Get results with all funcs applied or only those requested."
  ([run results-type]
     (let [sims (map get-doc (:results run))
           fields-funcs (get-fields-funcs run results-type :all)]
       (map (fn [sim] (zipmap (concat [:simulation]
                                      (if (:params (first (get sim results-type)))
                                        [:params] [:control-params :comparison-params])
                                      (map (fn [[field func]]
                                             (keyword (format "%s%s" (name field)
                                                              (str/capitalize func))))
                                           fields-funcs))
                              (concat
                               [(:simulation (first (get sim results-type)))]
                               (summarize-sim-results sim results-type fields-funcs))))
            sims)))
  ([run results-type fields-funcs]
     (let [sims (map get-doc (:results run))]
       (map (fn [sim]
              (zipmap
               (concat
                ["Simulation"]
                (if (:params (first (get sim results-type)))
                  [:params] [:control-params :comparison-params])
                (format-summary-fields fields-funcs))
               (concat
                [(format "%d: <a href=\"/simulation/%s\">%s</a>"
                         (:simulation sim) (:_id sim) (subs (:_id sim) 22))]
                (summarize-sim-results sim results-type fields-funcs))))
            sims))))
