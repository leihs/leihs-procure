(ns leihs.procurement.resources.building
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(defn building-query
  [id]
  (-> (sql/select :buildings.*)
      (sql/from :buildings)
      (sql/where [:= :buildings.id id])
      sql/format))

(defn get-building
  [context args value]
  (first (jdbc/query (-> context
                         :request
                         :tx)
                     (building-query (:building_id value)))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
