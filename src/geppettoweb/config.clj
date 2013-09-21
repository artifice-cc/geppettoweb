(ns geppettoweb.config
  (:use propertea.core)
  (:use [geppetto.misc]))

(def graphs-help (ref nil))

(defn load-config
  []
  (let [props (read-properties "config.properties")]
    (setup-geppetto (:geppetto_dbhost props)
                    (:geppetto_dbport props)
                    (:geppetto_dbname props)
                    (:geppetto_dbuser props)
                    (:geppetto_dbpassword props)
                    true)
    (dosync (alter graphs-help (constantly (:graphs_help props))))))

