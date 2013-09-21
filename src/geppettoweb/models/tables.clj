(ns geppettoweb.models.tables
  (:require [clojure.string :as str])
  (:use [korma.core])
  (:use [geppetto.runs :only [get-run]])
  (:use [geppetto.models])
  (:use [geppetto.misc])
  (:use [geppettoweb.models.common]))

(defentity table-fields
  (table :table_fields)
  (pk :tfid)
  (belongs-to runs {:fk :runid}))

(defn get-table-fields
  [runid tabletype]
  (set (map keyword (sort (map :field (with-db @geppetto-db
                                    (select table-fields (fields :field)
                                            (where {:runid runid
                                                    :tabletype (name tabletype)}))))))))

(defn set-table-fields
  [runid tabletype fields]
  (with-db @geppetto-db
    (delete table-fields (where {:runid runid :tabletype (name tabletype)}))
    (insert table-fields
            (values (map (fn [f] {:runid runid :tabletype (name tabletype) :field f})
                       fields)))))
