(ns geppettoweb.config
  (:use propertea.core)
  (:use [geppetto.misc]))

(def graphs-help (ref nil))

(def port (ref "3737"))

(defn load-config
  [config-file]
  (let [props (read-properties config-file)]
    (setup-geppetto (:geppetto_dbhost props)
                    (:geppetto_dbport props)
                    (:geppetto_dbname props)
                    (:geppetto_dbuser props)
                    (:geppetto_dbpassword props)
                    true)
    (dosync (alter port (constantly (:port props)))
            (alter graphs-help (constantly (:graphs_help props))))))

