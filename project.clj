(defproject cc.artifice/geppettoweb "2.5.0-SNAPSHOT"
  :description "Web-based viewer for Geppetto data."
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cc.artifice/geppetto "2.5.0-SNAPSHOT"]
                 [compojure "1.1.5"]
                 [ring/ring-core "1.1.7"]
                 [hiccup "1.0.4"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [korma "0.3.0-RC2"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [org.markdownj/markdownj "0.3.0-1.0.2b4"]
                 [fleet "0.9.5"]
                 [propertea "1.2.3"]]
  :plugins [[lein-ring "0.8.2"]]
  :profiles {:dev {:dependencies [[ring-mock "0.1.3"]]}}
  :ring {:handler geppettoweb.handler/app
         :init geppettoweb.config/load-config})

