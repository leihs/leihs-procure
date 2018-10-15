(ns leihs.procurement.backend.run
	(:refer-clojure :exclude [str keyword])
	(:require 
		[leihs.core.core :refer [keyword str presence]]

		[leihs.procurement env [handler :as handler] [status :as status]]
		[leihs.procurement.utils [ds :as ds]
		 [http-server :as http-server]]
		[leihs.procurement.utils.url [http :as http-url]
		 [jdbc :as jdbc-url]]

		[clj-pid.core :as pid]
		[clojure.pprint :refer [pprint]]
		[clojure.tools [cli :as cli] [logging :as logging]]
		[environ.core :as environ]

		[logbug.catcher :as catcher]))

(def defaults
  {:LEIHS_PROCURE_HTTP_BASE_URL "http://localhost:3230",
   :LEIHS_SECRET (when (= leihs.procurement.env/env :dev) "secret"),
   :LEIHS_DATABASE_URL
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
                  (let [status (status/init)]
                    (ds/init (:database-url options)
                             (:health-check-registry status)))
                  (let [secret (-> options
                                   :secret)
                        app-handler (handler/init secret)]
                    (http-server/start (:http-base-url options) app-handler))
                  (handle-pidfile)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn env-or-default [kw]
  (or (-> (System/getenv) (get (str kw) nil) presence)
      (get defaults kw nil)))

(defn extend-pg-params
  [params]
  (assoc params
    :password (or (:password params) (environ/env :pgpassword))
    :username (or (:username params) (environ/env :pguser))
    :port (or (:port params) (environ/env :pgport))))

(def cli-options
  [["-h" "--help"]
   ["-b" "--http-base-url LEIHS_PROCURE_HTTP_BASE_URL"
    (str "default: " (:LEIHS_PROCURE_HTTP_BASE_URL defaults)) 
    :default (http-url/parse-base-url (env-or-default :LEIHS_PROCURE_HTTP_BASE_URL)) 
    :parse-fn http-url/parse-base-url]
   ["-d" "--database-url LEIHS_DATABASE_URL"
    (str "default: " (:LEIHS_DATABASE_URL defaults)) :default
    (-> (env-or-default :LEIHS_DATABASE_URL)
        jdbc-url/dissect
        extend-pg-params) :parse-fn
    #(-> %
         jdbc-url/dissect
         extend-pg-params)]
   ["-s" "--secret LEIHS_SECRET" (str "default: " (:LEIHS_SECRET defaults))
    :default (env-or-default :LEIHS_SECRET)]])

(defn main-usage
  [options-summary & more]
  (->>
    ["Leihs procure run " "" "usage: leihs-procure run [<opts>] [<args>]" ""
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
