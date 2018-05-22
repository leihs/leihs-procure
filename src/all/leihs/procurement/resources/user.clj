(ns leihs.procurement.resources.user
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.resources.request :as request]
            [leihs.procurement.utils.sql :as sql]
            [leihs.procurement.utils.ds :refer [get-ds]]
            [logbug.debug :as debug]))

(def user-base-query
  (-> (sql/select :users.*)
      (sql/from :users)))

(defn get-user
  [context _ value]
  (first (jdbc/query (-> context
                         :request
                         :tx)
                     (-> user-base-query
                         (sql/where [:= :users.id (:user_id value)])
                         sql/format))))

(defn get-user-by-id
  [tx id]
  (first (jdbc/query tx
                     (-> user-base-query
                         (sql/where [:= :users.id id])
                         sql/format))))

(defn requested?
  [tx user r-id]
  (= (:id user)
     (->> r-id
          request/request-base-query
          (jdbc/query tx)
          first
          :user_id)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
