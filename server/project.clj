(defproject leihs-procurement "0.1.0-SNAPSHOT"
  :url "https://github.com/leihs/leihs-procure"
  :license {:name "GNU General Public License (GPL) v3",
            :url "http://www.gnu.org/licenses/gpl-3.0.txt"}
  :dependencies
    [[aleph "0.4.6"] [bidi "2.1.3"] [camel-snake-kebab "0.4.0"]
     [cheshire "5.8.0"] [cider-ci/open-session "2.0.0-beta.1"]
     [clj-http "3.8.0"] [clj-time "0.14.3"] [cljs-http "0.1.44"]
     [cljsjs/jimp "0.2.27"] [cljsjs/js-yaml "3.3.1-0"]
     [cljsjs/moment "2.17.1-1"] [clojure-humanize "0.2.2"] [clj-pid "0.1.2"]
     [com.github.mfornos/humanize-slim "1.2.2"]
     [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
     [com.walmartlabs/lacinia "0.28.0"] [compojure "1.6.0"] [environ "1.1.0"]
     [hiccup "1.0.5"] [hickory "0.7.1"] [hikari-cp "2.6.0"] [honeysql "0.9.2"]
     [inflections "0.13.0"] [io.forward/yaml "1.0.7"]
     [log4j/log4j "1.2.17" :exclusions
      [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
     [logbug "4.2.2"] [org.clojure/data.codec "0.1.1"]
     [io.dropwizard.metrics/metrics-core "4.0.3"]
     [io.dropwizard.metrics/metrics-healthchecks "4.0.3"]
     [org.clojure/clojure "1.9.0"]
     [org.clojure/clojurescript "1.10.217" :scope "provided"]
     [org.clojure/java.jdbc "0.7.5"] [org.clojure/tools.cli "0.3.5"]
     [org.clojure/tools.logging "0.4.0"] [org.clojure/tools.nrepl "0.2.13"]
     [org.clojure/clojure-contrib "1.2.0"] [org.slf4j/slf4j-log4j12 "1.7.25"]
     [org.slf4j/slf4j-log4j12 "1.7.25"] [pg-types "2.3.0"] [reagent "0.7.0"]
     [ring "1.6.3"] [ring-middleware-accept "2.0.3"] [ring/ring-json "0.4.0"]
     [timothypratley/patchin "0.3.5"] [threatgrid/ring-graphql-ui "0.1.1"]
     [uritemplate-clj "1.1.1"] [venantius/accountant "0.2.4"]
     [ring/ring-core "1.6.3"]]
  :plugins [[lein-zprint "0.3.8"] [lein-environ "1.1.0"]]
  :zprint {:width 80, :old? false, :map {:lift-ns? false}}
  ; jdk 9 needs ["--add-modules" "java.xml.bind"]
  :jvm-opts
    #=(eval (if (re-matches #"^9\..*" (System/getProperty "java.version"))
            ["--add-modules" "java.xml.bind"]
            []))
  ; :javac-options ["-target" "1.8" "-source" "1.8" "-xlint:-options"]
  :java-source-paths ["java"]
  :source-paths ["src/all"]
  :resource-paths ["resources/all"]
  :aot [#"leihs.procurement.*"]
  :target-path "target/%s"
  :main leihs.procurement.backend.main
  :uberjar-name "leihs-procurement.jar"
  :profiles {:dev [:project/dev :profiles/dev :profiles/dev+test],
             ;; including :base
             ;; (https://github.com/technomancy/leiningen/issues/1329)
             :test [:base :project/test :profiles/test :profiles/dev+test],
             :prod {:source-paths ["src/prod"],
                    :resource-paths ["resources/prod"],
                    :aot [#"leihs\..*"]},
             ;; -----------------------------------------------------------------
             ;; for local specific settings only edit :profiles/* in
             ;; profiles.clj
             :profiles/dev {},
             :profiles/test {},
             :profiles/dev+test {},
             ;; -----------------------------------------------------------------
             :project/dev {:source-paths ["src/dev" "src/dev+test"],
                           :resource-paths ["resources/dev"]},
             :project/test {:source-paths ["src/test" "src/dev+test"],
                            :resource-paths ["resources/test"],
                            :aot [#"leihs\..*"],
                            :env {:leihs-secret "secret"}}})
