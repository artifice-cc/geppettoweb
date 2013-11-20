(ns geppettoweb.handler
  (:require [compojure.handler :as handler]
            [compojure.route :as route])
  (:use [compojure.core])
  (:require [geppettoweb.views.run]
            [geppettoweb.views.tables]
            [geppettoweb.views.impacts]
            [geppettoweb.views.graphs]
            [geppettoweb.views.analyses]
            [geppettoweb.views.overview]
            [geppettoweb.views.parameters]
            [geppettoweb.views.results]))

(def app
  (handler/site (routes geppettoweb.views.run/run-routes
                        geppettoweb.views.tables/tables-routes
                        geppettoweb.views.impacts/impacts-routes
                        geppettoweb.views.graphs/graphs-routes
                        geppettoweb.views.analyses/analyses-routes
                        geppettoweb.views.overview/overview-routes
                        geppettoweb.views.parameters/parameters-routes
                        (route/resources "/")
                        (route/not-found "Not Found"))))

