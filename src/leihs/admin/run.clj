(ns leihs.admin.run
  (:refer-clojure :exclude [str keyword])
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.tools.cli :as cli :refer [parse-opts]]
    [leihs.admin.paths]
    [leihs.admin.routes :as routes]
    [leihs.admin.state :as state]
    [leihs.admin.utils.ssr]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.db :as db]
    [leihs.core.http-server :as http-server]
    [leihs.core.shutdown :as shutdown]
    [leihs.core.ssr-engine :as ssr-engine]
    [leihs.core.status :as status]
    [leihs.core.url.http :as http-url]
    [leihs.core.url.jdbc :as jdbc-url]
    [leihs.core.url.jdbc]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    [logbug.thrown :as thrown]
    [taoensso.timbre :refer [error warn info debug spy]]))


(defn run [options]
  (catcher/snatch
    {:return-fn (fn [e] (System/exit -1))}
    (info "Invoking run with options: " options)
    (shutdown/init options)
    (ssr-engine/init options)
    (leihs.core.ssr/init leihs.admin.utils.ssr/render-page-base)
    (let [status (status/init)]
      (db/init options (:health-check-registry status)))
    (state/init)
    (let [http-handler (routes/init)]
      (http-server/start options http-handler))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def cli-options
  (concat
    [["-h" "--help"]
     shutdown/pid-file-option]
    (http-server/cli-options :default-http-port 3220)
    db/cli-options))

(defn main-usage [options-summary & more]
  (->> ["Leihs PERM run "
        ""
        "usage: leihs-perm run [<opts>] [<args>]"
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
