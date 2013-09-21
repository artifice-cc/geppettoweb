(ns geppettoweb.models.common)

(defn to-clj
  [s]
  (try (read-string s)
       (catch Exception _)))
