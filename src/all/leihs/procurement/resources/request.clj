(ns leihs.procurement.resources.request
  (:require 
    [clj-logging-config.log4j :as logging-config]
    [clojure.tools.logging :as logging]
    [leihs.procurement.utils.sql :as sql]
    [clojure.java.jdbc :as jdbc]))

(defn request-base-query [{id :id}]
  (-> (sql/select :*)
      (sql/from :procurement_requests)
      (sql/where [:= :procurement_requests.id id])
      sql/format))

(defn get-request [context arguments]
  (first (jdbc/query (-> context :request :tx)
                     (request-base-query arguments))))

(defn requested-by-user? [{tx :tx} request user]
  (= (:user_id request) (:id user)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
