(ns leihs.procurement.resources.organization
  (:require
    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [leihs.procurement.utils.sql :as sql]
    [logbug.debug :as debug]
    ))

(defn organization-query [id]
  (-> (sql/select :procurement_organizations.*)
      (sql/from :procurement_organizations)
      (sql/where [:= :procurement_organizations.id id])
      sql/format))

(defn get-organization [context _ value]
  (first (jdbc/query (-> context :request :tx)
                     (organization-query (:organization_id value)))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
