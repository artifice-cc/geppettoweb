(ns geppettoweb.models.tables
  (:require [clojure.string :as str])
  (:use [korma db core])
  (:use [geppetto.runs :only [get-run]])
  (:use [geppetto.models])
  (:use [geppetto.misc])
  (:use [geppettoweb.models.common]))

(defn get-table-fields
  [runid tabletype]
  (with-db @geppetto-db
    (set (map keyword (sort (map :field (select table-fields (fields :field)
                                                (where {:runid runid
                                                        :tabletype (name tabletype)}))))))))

(defn set-table-fields
  [runid tabletype fields]
  (with-db @geppetto-db
    (delete table-fields (where {:runid runid :tabletype (name tabletype)}))
    (insert table-fields
            (values (map (fn [f] {:runid runid :tabletype (name tabletype) :field f})
                         fields)))))
