(ns leihs.admin.utils.ds
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.admin.utils.core :refer [keyword str presence]])
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :as logging]
    [hikari-cp.core :as hikari]
    [pg-types.all]
    [ring.util.codec]

    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug :refer [I> I>> identity-with-logging]]
    [logbug.ring :refer [wrap-handler-with-logging]]
    [logbug.thrown :as thrown]
    )
  (:import
    [java.net.URI]
    ))


(defonce ds (atom nil))
(defn get-ds [] @ds)

(defn wrap-tx [handler]
  (fn [request]
    (jdbc/with-db-transaction [tx @ds]
      (try 
        (let [resp (handler (assoc request :tx tx))]
          (when-let [status (:status resp)]
            (when (>= status 400 )
              (logging/warn "Rolling back transaction because error status " status)
              (jdbc/db-set-rollback-only! tx)))
          resp)
        (catch Throwable th
          (logging/warn "Rolling back transaction because of " th)
          (jdbc/db-set-rollback-only! tx)
          (throw th))))))


(defn init [params]
  (when @ds
    (do
      (logging/info "Closing db pool ...")
      (-> @ds :datasource hikari/close-datasource)
      (reset! ds nil)
      (logging/info "Closing db pool done.")))
  (logging/info "Initializing db pool " params " ..." )
  (reset!
    ds
    {:datasource
     (hikari/make-datasource
       {:auto-commit        true
        :read-only          false
        :connection-timeout 30000
        :validation-timeout 5000
        :idle-timeout       600000
        :max-lifetime       (* 3 60 60 1000)
        :minimum-idle       10
        :maximum-pool-size  (-> params :max-pool-size presence (or 5))
        :pool-name          "db-pool"
        :adapter            "postgresql"
        :username           (:username params)
        :password           (:password params)
        :database-name      (:database params)
        :server-name        (:host params)
        :port-number        (:port params)
        :register-mbeans    false})})
  (logging/info "Initializing db pool done.")
  @ds)

;;### Debug ####################################################################
;(debug/debug-ns *ns*)
;(logging-config/set-logger! :level :debug)
;(logging-config/set-logger! :level :info)
