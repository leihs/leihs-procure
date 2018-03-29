(ns leihs.procurement.backend.run
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.procurement.utils.core :refer [keyword str presence]])
  (:require
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
    ))

(def defaults
  {:LEIHS_HTTP_BASE_URL "http://localhost:3211"
   :LEIHS_SECRET (when (= leihs.procurement.env/env :dev) "secret")
   :LEIHS_DATABASE_URL "jdbc:postgresql://mkmit:mkmit@localhost:5432/leihs_dev?max-pool-size=5"
   })

(defn run [options]
  (catcher/snatch
    {:return-fn (fn [e] (System/exit -1))}
    (logging/info "Invoking run with options: " options)
    (when (nil? (:secret options))
      (throw (IllegalStateException. "LEIHS_SECRET resp. secret must be present!")))
    (let [ds (ds/init (:database-url options))
          secret (-> options :secret)
          app-handler (routes/init secret)
          http-server (http-server/start (:http-base-url options) app-handler)
          ])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn env-or-default [kw]
  (or (-> (System/getenv) (get (str kw) nil) presence)
      (get defaults kw nil)))

(defn extend-pg-params [params]
  (assoc params
         :password (or (:password params)
                       (System/getenv "PGPASSWORD"))
         :username (or (:username params)
                       (System/getenv "PGUSER"))
         :port (or (:port params)
                   (System/getenv "PGPORT"))))

(def cli-options
  [["-h" "--help"]
   ["-b" "--http-base-url LEIHS_HTTP_BASE_URL"
    (str "default: " (:LEIHS_HTTP_BASE_URL defaults))
    :default (http-url/parse-base-url (env-or-default :LEIHS_HTTP_BASE_URL))
    :parse-fn http-url/parse-base-url]
   ["-d" "--database-url LEIHS_DATABASE_URL"
    (str "default: " (:LEIHS_DATABASE_URL defaults))
    :default (-> (env-or-default :LEIHS_DATABASE_URL)
                 jdbc-url/dissect extend-pg-params)
    :parse-fn #(-> % jdbc-url/dissect extend-pg-params)]
   ["-s" "--secret LEIHS_SECRET"
    (str "default: " (:LEIHS_SECRET defaults))
    :default (env-or-default :LEIHS_SECRET)]])

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

(defn -main [& args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options :in-order true)
        pass-on-args (->> [options (rest arguments)]
                          flatten
                          (into []))]
    (cond
      (:help options) (println (main-usage summary {:args args :options options}))
      :else (run options))))

;(-main "-h")
;(-main)
