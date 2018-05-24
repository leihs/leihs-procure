(ns leihs.procurement.backend.run
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.procurement.utils.core :refer [keyword str presence]])
  (:require [environ.core :as environ]
            [leihs.procurement.routes :as routes]
            [leihs.procurement.env]
            [leihs.procurement.paths]
            [leihs.procurement.utils.ds :as ds]
            [leihs.procurement.utils.http-server :as http-server]
            [leihs.procurement.utils.url.http :as http-url]
            [leihs.procurement.utils.url.jdbc :as jdbc-url]
            [leihs.procurement.utils.url.jdbc]
            [clojure.tools.cli :as cli :refer [parse-opts]]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as logging]
            [logbug.catcher :as catcher]
            [logbug.debug :as debug]
            [logbug.thrown :as thrown]
            [clj-pid.core :as pid]))

(def defaults
  {:leihs-http-base-url "http://localhost:3211",
   :leihs-secret (when (= leihs.procurement.env/env :dev) "secret"),
   :leihs-database-url
     "jdbc:postgresql://leihs:leihs@localhost:5432/leihs?max-pool-size=5"})

(defn handle-pidfile
  []
  (let [pid-file "./tmp/server_pid"]
    (.mkdirs (java.io.File. "./tmp"))
    (pid/save pid-file)
    (pid/delete-on-shutdown! pid-file)))

(defn run
  [options]
  (catcher/snatch {:return-fn (fn [e] (System/exit -1))}
                  (logging/info "Invoking run with options: " options)
                  (when (nil? (:secret options))
                    (throw (IllegalStateException.
                             "LEIHS_SECRET resp. secret must be present!")))
                  (let
                    [ds (ds/init (:database-url options)) secret
                     (-> options
                         :secret) app-handler (routes/init secret) http-server
                     (http-server/start (:http-base-url options) app-handler)])
                  (handle-pidfile)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn env-or-default [kw] (or (environ/env kw) (get defaults kw nil)))

(defn extend-pg-params
  [params]
  (assoc params
    :password (or (:password params) (environ/env :pgpassword))
    :username (or (:username params) (environ/env :pguser))
    :port (or (:port params) (environ/env :pgport))))

(def cli-options
  [["-h" "--help"]
   ["-b" "--http-base-url LEIHS_HTTP_BASE_URL"
    (str "default: " (:leihs-http-base-url defaults)) :default
    (http-url/parse-base-url (env-or-default :leihs-http-base-url)) :parse-fn
    http-url/parse-base-url]
   ["-d" "--database-url LEIHS_DATABASE_URL"
    (str "default: " (:leihs-database-url defaults)) :default
    (-> (env-or-default :leihs-database-url)
        jdbc-url/dissect
        extend-pg-params) :parse-fn
    #(-> %
         jdbc-url/dissect
         extend-pg-params)]
   ["-s" "--secret LEIHS_SECRET" (str "default: " (:leihs-secret defaults))
    :default (env-or-default :leihs-secret)]])

(defn main-usage
  [options-summary & more]
  (->>
    ["Leihs PERM run " "" "usage: leihs-perm run [<opts>] [<args>]" ""
     "Options:" options-summary "" ""
     (when more
       ["-------------------------------------------------------------------"
        (with-out-str (pprint more))
        "-------------------------------------------------------------------"])]
    flatten
    (clojure.string/join \newline)))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]}
          (cli/parse-opts args cli-options :in-order true)
        pass-on-args (->> [options (rest arguments)]
                          flatten
                          (into []))]
    (cond (:help options) (println (main-usage summary
                                               {:args args, :options options}))
          :else (run options))))

;(-main "-h")
;(-main)
