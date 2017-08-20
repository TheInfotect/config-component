(defproject org.volenta/datamos.config "0.1.6.0"
  :description "Config module for dataMos."
  :url "http://theinfotect.org/datamos"
  :license {:name "GNU AFFERO GENERAL PUBLIC LICENSE, Version 3"
            :url  "https://www.gnu.org/licenses/agpl-3.0.nl.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.volenta/datamos "0.1.6.0"]
                 [mount "0.1.11"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.clojure/tools.namespace "0.2.11"]]
  :main ^:skip-aot datamos.config.core
  :test-paths ["test"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[org.clojure/test.check "0.10.0-alpha2"]]}}
  :repositories [["releases" {:url "https://clojars.org/repo"
                              :creds :gpg}]]
  :repl-options {:host "127.0.0.1"
                 :port 12311})
