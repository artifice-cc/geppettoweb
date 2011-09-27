(defproject sisyphus "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [noir "1.1.1-SNAPSHOT"]
                 [com.ashafa/clutch "0.2.4"]]
  :dev-dependencies [[swank-clojure "1.3.0"]
                     [midje "1.1"]
                     [clj-stacktrace "0.2.1"]
                     [lein-marginalia "0.6.0"]
                     [lein-ring "0.4.5"]]
  :ring {:handler sisyphus.server/handler}
  :main sisyphus.server)

