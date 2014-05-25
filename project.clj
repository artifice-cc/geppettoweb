(defproject cc.artifice/geppettoweb "3.0.0-SNAPSHOT"
  :description "Web-based viewer for Geppetto data."
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cc.artifice/geppetto "3.0.0-SNAPSHOT"]
                 [compojure "1.1.8"]
                 [ring/ring-core "1.2.2"]
                 [hiccup "1.0.5"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [korma "0.3.1"]
                 [mysql/mysql-connector-java "5.1.30"]
                 [org.pegdown/pegdown "1.4.2"]
                 [cc.artifice/fleet "0.10.2"]
                 [propertea "1.3.1"]
                 [environ "0.5.0"]]
  :plugins [[lein-ring "0.8.2"]]
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}}
  :ring {:handler geppettoweb.handler/app})

