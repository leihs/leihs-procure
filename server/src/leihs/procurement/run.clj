(ns leihs.procurement.run
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli :refer [parse-opts]]
    [clojure.tools.logging :as logging]
    [leihs.core.db :as db]
    [leihs.core.http-server :as http-server]
    [leihs.core.json-protocol]
    [leihs.core.shutdown :as shutdown]
    [leihs.core.status :as status]
    [leihs.procurement.graphql :as graphql]
    [leihs.procurement.routes :as routes]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    [logbug.thrown :as thrown]
    ))


(defn run [options]
  (catcher/snatch
    {:return-fn (fn [e] (System/exit -1))}
    (logging/info "Invoking run with options: " options)
    (shutdown/init options)
    (graphql/init)
    (let [status (status/init)]
      (db/init options (:health-check-registry status)))
    (let [http-handler (routes/init)]
      (http-server/start options http-handler))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def cli-options
  (concat
    [["-h" "--help"]
     shutdown/pid-file-option]
    (http-server/cli-options :default-http-port 3230)
    db/cli-options))

(defn main-usage [options-summary & more]
  (->> ["Leihs Procurement run "
        ""
        "usage: leihs-procurement run [<opts>] [<args>]"
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

