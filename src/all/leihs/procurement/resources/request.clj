(ns leihs.procurement.resources.request
  (:require 
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [leihs.procurement.utils.sql :as sql]
    [clojure.java.jdbc :as jdbc]))

(defn request-query [id]
  (-> (sql/select :*)
      (sql/from :procurement_requests)
      (sql/where [:= :procurement_requests.id (sql/call :cast id :uuid)])
      sql/format))

(defn requests-by-requester-query [user-id]
  (-> (sql/select :*)
      (sql/from :procurement_requests)
      (sql/where [:= :procurement_requests.user_id (sql/call :cast user-id :uuid)])
      sql/format))

(defn requests-not-by-requester-query [user-id]
  (-> (sql/select :*)
      (sql/from :procurement_requests)
      (sql/where [:<> :procurement_requests.user_id (sql/call :cast user-id :uuid)])
      sql/format))

(defn get-request [{tx :tx} id]
  (first (jdbc/query tx (request-query id))))

(defn get-requests-by-requester [{tx :tx, auth-entity :authenticated-entity}]
  (jdbc/query tx (requests-by-requester-query (:id auth-entity))))

(defn get-requests-not-by-requester [{tx :tx, auth-entity :authenticated-entity}]
  (jdbc/query tx (requests-not-by-requester-query (:id auth-entity))))

(defn requested-by-user? [{tx :tx} request user]
  (= (:user_id request) (:id user)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
