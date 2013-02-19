(ns sisyphus.models.common)

(def sisyphus-db (ref nil))

(defn to-clj
  [s]
  (try (read-string s)
       (catch Exception _)))
