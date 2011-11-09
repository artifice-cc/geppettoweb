(ns sisyphus.models.runs
  (:require [clojure.set :as set])
  (:require [clojure.string :as str])
  (:require [clojure.java.io :as io])
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
    (doseq [r (concat (:comparative run) (:control run) (:comparison run))]
      (delete-doc (get-doc r)))
    (delete-doc run)))

(defn add-annotation
  [id content]
  (clutch/with-db db (clutch/update-document (get-doc id) #(conj % content) [:annotations])))

(defn delete-annotation
  [id index]
  (let [annotations (:annotations (clutch/get-document id))]
    (clutch/with-db db
      (clutch/update-document
       (get-doc id) {:annotations (concat (take index annotations)
                                          (drop (inc index) annotations))}))))

(defn set-graphs
  [id graphs]
  (clutch/with-db db
    (clutch/update-document (get-doc id) {:graphs graphs})))

(defn set-analysis
  [id analysis]
  (clutch/with-db db
    (clutch/update-document (get-doc id) {:analysis analysis})))

(defn get-summary-fields
  [run results-type & opts]
  (get-simulation-fields (get-doc (first (:results run))) results-type opts))

(defn format-summary-fields
  [fields-funcs]
  (map (fn [[field func]] (format "%s (%s)" (name field) func)) fields-funcs))

(defn set-summary-fields)

(defn get-summary-results
  [run results-type fields-funcs]
  (let [sims (map get-doc (:results run))]
    (map (fn [sim]
           (zipmap
            (concat
             ["Simulation"]
             (if (= :control results-type)
               [:params] [:control-params :comparison-params])
             (format-summary-fields fields-funcs))
            (concat
             [(format "<a href=\"/simulation/%s\">%s</a>"
                      (:_id sim) (subs (:_id sim) 22))]
             (if (= :control results-type)
               [(:params (first (get sim results-type)))]
               [(:control-params (first (get sim results-type)))
                (:comparison-params (first (get sim results-type)))])
             (for [[field func] fields-funcs]
               (let [vals (map field (get sim results-type))]
                 (cond (= func "sum")
                       (reduce + 0 vals)
                       (= func "avg")
                       (double (/ (reduce + 0 vals) (count vals)))
                       :else (reduce + 0 vals)))))))
         sims)))

(def cachedir "/tmp")

(defn csv-filenames
  [run]
  (let [results (if (= "comparative" (:paramstype run))
                  [:control :comparison :comparative]
                  [:control])]
    (zipmap results (map #(format "%s/%s-%s.csv" cachedir (:_id run) (name %)) results))))

(defn format-csv-row
  [row]
  ;; add quotes around string data
  (let [fmt (fn [s] (format "\"%s\"" (str/replace s "\"" "\\\"")))]
    (apply str (concat (interpose "," (map #(cond (= String (type %)) (fmt %)
                                                  (map? %) (fmt (pr-str %))
                                                  :else %)
                                           row))
                       [\newline]))))

(defn results-to-csv
  [run csv-fnames]
  (doseq [results-type (keys csv-fnames)]
    (let [outfile (io/file (get csv-fnames results-type))]
      (when (. outfile createNewFile)
        (let [results (get-summary-results (:_id run) results-type)
              fields (get-summary-fields results :all)
              csv (apply str (map (fn [r] (format-csv-row (map (fn [f] (get r f)) fields)))
                                  results))]
          ;; save into cache file
          (with-open [writer (io/writer outfile)]
            (.write writer (format-csv-row (map name fields)))
            (.write writer csv)))))))
