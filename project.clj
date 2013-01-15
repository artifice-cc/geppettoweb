(defproject sisyphus "1.0.0"
  :description "FIXME: write this!"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [noir "1.2.0"]
                 [cc.artifice/granary "0.1.0-SNAPSHOT"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [korma "0.3.0-beta15-SNAPSHOT"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [org.markdownj/markdownj "0.3.0-1.0.2b4"]]
  :dev-dependencies [[lein-ring "0.4.5"]]
  :ring {:handler sisyphus.server/handler}
  :main sisyphus.server
  :jvm-opts ["-Xmx1200m"])

