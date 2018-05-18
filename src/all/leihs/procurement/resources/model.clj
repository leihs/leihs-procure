(ns leihs.procurement.resources.model
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(defn model-query
  [id]
  (-> (sql/select :models.*)
      (sql/from :models)
      (sql/where [:= :models.id id])
      sql/format))

(defn get-model-by-id [tx id] (first (jdbc/query tx (model-query id))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
