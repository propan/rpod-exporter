(defproject rpod-exporter "0.1.0-SNAPSHOT"
  :description "a tool to export podcast data from RPOD.ru"
  :url "http://github.com/prokpa/rpod-exporter"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[enlive "1.1.5"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/tools.cli "0.3.1"]]
  :main ^:skip-aot rpod-exporter.app
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
