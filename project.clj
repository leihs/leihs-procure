(defproject procurement-graphql "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.walmartlabs/lacinia "0.21.0"]
                 [org.clojure/java.jdbc "0.7.1"],
                 [org.postgresql/postgresql "42.1.4"],
                 [honeysql "0.9.2"]]
  :main ^:skip-aot procurement-graphql.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :plugins [[lein-exec "0.3.7"]])
