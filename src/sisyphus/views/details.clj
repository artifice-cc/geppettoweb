(ns sisyphus.views.details
  (:require [sisyphus.views.common :as common])
  (:use noir.core hiccup.core hiccup.page-helpers)
  (:use [sisyphus.models.runs :only [get-run]]))

(defpartial format-field
  [field val]
  (if (= field :runid)
    (link-to (format "/details/%s" val) val)
    val))

(defpartial details-table
  [details]
  [:table (map (fn [k] [:tr [:td k] [:td (format-field k (k details))]])
               (sort (keys details)))])

(defpage "/details/:id" {id :id}
  (details-table (get-run id)))
