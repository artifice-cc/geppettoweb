(defproject cc.artifice/sisyphus "1.0.0"
  :description "Web-based viewer for Granary data."
  :dependencies [[noir "1.2.0"]
                 [cc.artifice/geppetto "2.4.0-SNAPSHOT"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [korma "0.3.0-RC2"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [org.markdownj/markdownj "0.3.0-1.0.2b4"]
                 [fleet "0.9.5"]
                 [propertea "1.2.3"]]
  :dev-dependencies [[lein-ring "0.4.5"]]
  :ring {:handler sisyphus.server/handler}
  :main sisyphus.server)

