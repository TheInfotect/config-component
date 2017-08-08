(defproject org.volenta/datamos.config "0.1.0-SNAPSHOT"
  :description "Config module for dataMos."
  :url "http://theinfotect.org/datamos"
  :license {:name "GNU AFFERO GENERAL PUBLIC LICENSE, Version 3"
            :url  "https://www.gnu.org/licenses/agpl-3.0.nl.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.volenta/datamos "0.1.3"]
                 [mount "0.1.11"]]
  :main datamos.config.core
  :aot [datamos.config.core]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[org.clojure/test.check "0.10.0-alpha2"]]}})