(ns geppettoweb.handler
  (:require [compojure.handler :as handler]
            [compojure.route :as route])
  (:require [environ.core])
  (:use [propertea.core :only [read-properties]])
  (:use [geppetto.misc :only [setup-geppetto]])
  (:use [compojure.core])
  (:use [geppettoweb.state])
  (:require [geppettoweb.views.run]
            [geppettoweb.views.tables]
            [geppettoweb.views.impacts]
            [geppettoweb.views.graphs]
            [geppettoweb.views.analyses]
            [geppettoweb.views.overview]
            [geppettoweb.views.parameters]
            [geppettoweb.views.results]))

(defroutes app-routes
  geppettoweb.views.run/run-routes
  geppettoweb.views.tables/tables-routes
  geppettoweb.views.impacts/impacts-routes
  geppettoweb.views.graphs/graphs-routes
  geppettoweb.views.analyses/analyses-routes
  geppettoweb.views.overview/overview-routes
  geppettoweb.views.parameters/parameters-routes
  (route/resources "/"))

(def app
  (let [config-file (if-let [config-name (environ.core/env :config)]
                      (format "%s-config.properties" config-name)
                      "config.properties")
        props (read-properties config-file)]
    (setup-geppetto (:geppetto_dbhost props)
                    (:geppetto_dbport props)
                    (:geppetto_dbname props)
                    (:geppetto_dbuser props)
                    (:geppetto_dbpassword props)
                    true)
    (when (:context props)
      (dosync (alter app-context (constantly (:context props)))))
    (when (:title props)
      (dosync (alter app-title (constantly (:title props)))))
    (handler/site
     (routes
      (context (:context props) [] app-routes)
      (route/not-found "Not Found")))))
