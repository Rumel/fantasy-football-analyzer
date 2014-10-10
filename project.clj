(defproject fantasy-football-analyzer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.6.0"]
                  [enlive "1.1.5"]
                  [org.clojure/tools.logging "0.3.1"]]
  :main ^:skip-aot fantasy-football-analyzer.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
