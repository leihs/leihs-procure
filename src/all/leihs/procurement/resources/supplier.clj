(ns leihs.procurement.resources.supplier
  (:require
    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [leihs.procurement.utils.sql :as sql]
    [logbug.debug :as debug]
    ))

(defn supplier-query [id]
  (-> (sql/select :suppliers.*)
      (sql/from :suppliers)
      (sql/where [:= :suppliers.id id])
      sql/format))

(defn get-supplier [context _ value]
  (first (jdbc/query (-> context :request :tx)
                     (supplier-query (:supplier_id value)))))

;#### debug ###################################################################
(logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
