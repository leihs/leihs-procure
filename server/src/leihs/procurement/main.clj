(ns leihs.procurement.main
  (:require
   [clj-yaml.core :as yaml]
   [clojure.pprint :refer [pprint]]
   [clojure.tools.cli :as cli :refer [parse-opts]]
   [environ.core :refer [env]]
   [leihs.core.logging]
   [leihs.core.reload :as reload]
   [leihs.core.repl :as repl]
   [leihs.procurement.run :as run]
   [logbug.catcher :as catcher]
   [logbug.debug :as debug]
   [logbug.thrown :as thrown]
   [taoensso.timbre :refer [debug info warn error]])
  (:gen-class))

(thrown/reset-ns-filter-regex #"^(leihs|cider)\..*")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
   [["-h" "--help"]
    [nil "--dev-mode DEV_MODE" "dev mode"
     :default (or (some-> :dev-mode env yaml/parse-string) false)
     :parse-fn #(yaml/parse-string %)
     :validate [boolean? "Must parse to a boolean"]]]
   repl/cli-options))

(defn main-usage [options-summary & more]
  (->> ["Leihs Procure"
        ""
        "usage: leihs-procure [<opts>] SCOPE [<scope-opts>] [<args>]"
        ""
        "Options:"
        options-summary
        ""
        ""
        (when more
          ["-------------------------------------------------------------------"
           (with-out-str (pprint more))
           "-------------------------------------------------------------------"])]
       flatten (clojure.string/join \newline)))

(defn main [args]
  (leihs.core.logging/init)
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        options (into (sorted-map) options)]
    (repl/init options)
    (cond
      (:help options) (println (main-usage summary {:args args :options options}))
      :else (case (-> arguments first keyword)
              :run (run/main options (rest arguments))
              (println (main-usage summary {:args args :options options}))))))

(defn -main [& args]
  (info 'main args)
  (reset! reload/args* args)
  (main args))

; dynamic restart on require
(when @reload/args* (main @reload/args*))

;(-main "-h")
;(-main "run")
