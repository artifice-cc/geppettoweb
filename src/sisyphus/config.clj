(ns sisyphus.config
  (:use propertea.core)
  (:use [granary.misc])
  (:use [korma.db :only [create-db mysql]])
  (:use [sisyphus.models.common :only [sisyphus-db]]))

(def graphs-help (ref nil))

(def port (ref "3737"))

(def cachedir (ref "cache"))

(defn load-config
  []
  (let [props (read-properties "config.properties")]
    (prn props)
    (set-granary-db (:granary_dbhost props)
                    (:granary_dbname props)
                    (:granary_dbuser props)
                    (:granary_dbpassword props))
    (dosync (alter cachedir (constantly (:cachedir props)))
            (alter port (constantly (:port props)))
            (alter graphs-help (constantly (:graphs_help props)))
            (alter sisyphus-db
                   (constantly
                    (create-db (mysql {:host (:sisyphus_dbhost props)
                                       :db (:sisyphus_dbname props)
                                       :user (:sisyphus_dbuser props)
                                       :password (:sisyphus_dbpassword props)})))))))

