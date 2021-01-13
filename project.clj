(load-file "shared-clj/deps.clj")

(defproject leihs-admin "0.0.0"
  :description "Admin Service for Leihs"
  :url "https://github.com/leihs/leihs-admin"
  :license {:name "AGPL"
            :url "https://www.gnu.org/licenses/agpl-3.0.de.html "}
  :dependencies ~(extend-shared-deps '[])

  ; jdk 9 needs ["--add-modules" "java.xml.bind"]
  :jvm-opts #=(eval (if (re-matches #"^(9|10)\..*" (System/getProperty "java.version"))
                      ["--add-modules" "java.xml.bind"]
                      []))

  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]

  :source-paths ["src/all" "shared-clj/src"]

  :resource-paths ["resources/all"]

  :test-paths ["src/test"]

  :aot [#"leihs\..*"]

  :main leihs.admin.main

  :plugins [[lein-asset-minifier "0.4.4" :exclusions [org.clojure/clojure]]
            [lein-environ "1.1.0"]
            [lein-shell "0.4.2"]]

  :aliases {"auto-reset" ["auto" "exec" "-p" "scripts/lein-exec-reset.clj"]}

  :profiles {:dev-common
             {:dependencies [[com.cemerick/piggieback "0.2.2"]
                             [org.clojure/tools.nrepl "0.2.13"]
                             [pjstadig/humane-test-output "0.8.3"]
                             [prone "1.6.0"]
                             [ring/ring-devel "1.6.3"]
                             [ring/ring-mock "0.3.2"]]
              :plugins [[lein-auto "0.1.3"]
                        [lein-exec "0.3.7"]
                        [lein-sassy "1.0.8"]]
              :source-paths ["src/all" "src/dev" "shared-clj/src"]
              :resource-paths ["resources/all" "resources/dev" "target/cljsbuild"]
              :injections [(require 'pjstadig.humane-test-output)
                           (pjstadig.humane-test-output/activate!)]
              :env {:dev true}}
             :dev-overrides {} ; defined if needed in profiles.clj file
             :dev [:dev-common :dev-overrides]
             :uberjar {:hooks [minify-assets.plugin/hooks]
                       :source-paths ["src/all" "src/prod" "shared-clj/src"]
                       :prep-tasks [["shell" "./bin/build-timestamp"] "compile" ]
                       :resource-paths ["resources/all" "resources/prod"]
                       :aot [#"leihs\..*"]
                       :uberjar-name "leihs-admin.jar"}
             :test {:resource-paths ["resources/all" "resources/test"]}}


)
