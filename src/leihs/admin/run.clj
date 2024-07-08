(ns leihs.admin.run
  (:refer-clojure :exclude [str keyword])
  (:require
   [clj-yaml.core :as yaml]
   [clojure.pprint :refer [pprint]]
   [clojure.tools.cli :as cli :refer [parse-opts]]
   [environ.core :refer [env]]
   [leihs.admin.paths]
   [leihs.admin.routes :as routes]
   [leihs.admin.state :as state]
   [leihs.core.cli :refer [long-opt-for-key]]
   [leihs.core.db :as db]
   [leihs.core.http-server :as http-server]
   [leihs.core.shutdown :as shutdown]
   [leihs.core.status :as status]
   [leihs.core.url.jdbc]
   [logbug.catcher :as catcher]
   [taoensso.timbre :refer [info]]))

(defn run [options]
  (catcher/snatch
   {:return-fn (fn [e] (System/exit -1))}
   (info "Invoking run with options: " options)
   (shutdown/init options)
   (let [status (status/init)]
     (db/init options (:health-check-registry status)))
   (state/init)
   (let [http-handler (routes/init options)]
     (http-server/start options http-handler))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def cli-options
  (concat
   [["-h" "--help"]
    shutdown/pid-file-option
    [nil  (long-opt-for-key :enable-auth-header-prefix-basic)
     "enable basic auth; either true of false (parsed via YAML)"
     :default (yaml/parse-string
               (or (some-> :enable-auth-header-prefix-basic env)
                   "true"))
     :parse-fn yaml/parse-string]]
   (http-server/cli-options :default-http-port 3220)
   db/cli-options))

(defn main-usage [options-summary & more]
  (->> ["leihs-admin run "
        ""
        "usage: leihs-admin run [<opts>] [<args>]"
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

(defn main [gopts args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        pass-on-args (->> [options (rest arguments)]
                          flatten (into []))
        options (merge gopts options)]
    (cond
      (:help options) (println (main-usage summary {:args args :options options}))
      :else (run options))))

