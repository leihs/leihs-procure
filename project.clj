(defproject leihs-procurement "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies
  [
   [aleph "0.4.4"]
   [bidi "2.1.3"]
   [camel-snake-kebab "0.4.0"]
   [cheshire "5.8.0"]
   [cider-ci/open-session "2.0.0-beta.1"]
   [clj-http "3.8.0"]
   [cljs-http "0.1.44"]
   [cljsjs/jimp "0.2.27"]
   [cljsjs/js-yaml "3.3.1-0"]
   [cljsjs/moment "2.17.1-1"]
   [clojure-humanize "0.2.2"]
   [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
   [com.mchange/c3p0 "0.9.5.2"]
   [com.walmartlabs/lacinia "0.21.0"]
   [compojure "1.6.0"]
   [environ "1.1.0"]
   [hiccup "1.0.5"]
   [hickory "0.7.1"]
   [honeysql "0.9.2"]
   [inflections "0.13.0"]
   [io.forward/yaml "1.0.7"]
   [log4j/log4j "1.2.17" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
   [logbug "4.2.2"]
   [org.clojure/data.codec "0.1.1"]
   [org.clojure/clojure "1.9.0"]
   [org.clojure/clojurescript "1.10.217" :scope "provided"]
   [org.clojure/java.jdbc "0.7.5"]
   [org.clojure/tools.cli "0.3.5"]
   [org.clojure/tools.logging "0.4.0"]
   [org.clojure/tools.nrepl "0.2.13"]
   [org.slf4j/slf4j-log4j12 "1.7.25"]
   [org.slf4j/slf4j-log4j12 "1.7.25"]
   [pg-types "2.3.0"]
   [reagent "0.7.0"]
   [ring "1.6.3"]
   [ring-middleware-accept "2.0.3"]
   [ring/ring-json "0.4.0"]
   [timothypratley/patchin "0.3.5"]
   [threatgrid/ring-graphql-ui "0.1.1"]
   [uritemplate-clj "1.1.1"]
   [venantius/accountant "0.2.4"]

   ; force transitive dependency resolution
   [ring/ring-core "1.6.3"]
   ]

  ; jdk 9 needs ["--add-modules" "java.xml.bind"]
  :jvm-opts #=(eval (if (re-matches #"^9\..*" (System/getProperty "java.version"))
                      ["--add-modules" "java.xml.bind"]
                      []))

  ; :javac-options ["-target" "1.8" "-source" "1.8" "-xlint:-options"]

  :target-path "target/%s"
  :main leihs.procurement.backend.main
  :profiles {:dev {:source-paths ["src/all" "src/dev"]
                   :resource-paths ["resources/all" "resources/dev"]
                   :env {:dev true}}
             :uberjar {:source-paths ["src/all" "src/prod"]
                       :resource-paths ["resources/all" "resources/prod"]
                       :aot [#"leihs\..*"]
                       :uberjar-name "leihs-procurement.jar"}}
  )
