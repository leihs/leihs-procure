(load-file "./shared-clj/deps.clj")

(defproject leihs-procurement "0.1.0-SNAPSHOT"
  :url "https://github.com/leihs/leihs-procure"
  :license {:name "GNU General Public License (GPL) v3",
            :url "http://www.gnu.org/licenses/gpl-3.0.txt"}
  :dependencies
    ~(extend-shared-deps '[[camel-snake-kebab "0.4.0"]
                           [cider-ci/open-session "2.0.0-beta.1"]
                           [clj-http "3.8.0"]
                           [clojure-humanize "0.2.2"]
                           [clj-pid "0.1.2"]
                           [com.github.mfornos/humanize-slim "1.2.2"]
                           [com.walmartlabs/lacinia "0.31.0"]
                           [environ "1.1.0"]
                           [hickory "0.7.1"]
                           [inflections "0.13.0"]
                           [org.clojure/clojure "1.9.0"]
                           [org.clojure/tools.cli "0.3.5"]
                           [org.clojure/tools.nrepl "0.2.13"]
                           [org.clojure/clojure-contrib "1.2.0"]
                           [timothypratley/patchin "0.3.5"]
                           [threatgrid/ring-graphql-ui "0.1.1"]
                           [uritemplate-clj "1.1.1"]])
  :plugins [[lein-zprint "0.3.8"] [lein-environ "1.1.0"]]
  :aliases {"auto-reset" ["auto" "exec" "-p" "scripts/lein-exec-reset.clj"]}
  :jvm-opts
    #=(eval (if (re-matches #"^(9|10)\..*" (System/getProperty "java.version"))
            ["--add-modules" "java.xml.bind"]
            []))
  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]
  :java-source-paths ["java"]
  :source-paths ["src/all" "shared-clj/src"]
  :resource-paths ["resources/all"]
  :aot [#"leihs.procurement.*"]
  :main leihs.procurement.backend.main
  :profiles {:dev [:project/dev :profiles/dev],
             ;; -----------------------------------------------------------------
             ;; for local specific settings only edit :profiles/* in
             ;; profiles.clj
             :profiles/dev {},
             ;; -----------------------------------------------------------------
             :project/dev {:source-paths ["src/dev"],
                           :resource-paths ["resources/dev"],
                           :plugins [[lein-auto "0.1.3"]
                                     [lein-exec "0.3.7"]]},
             :uberjar {:source-paths ["src/prod"],
                       :resource-paths ["resources/prod"],
                       :aot [#"leihs\..*"],
                       :uberjar-name "leihs-procure.jar"}})
