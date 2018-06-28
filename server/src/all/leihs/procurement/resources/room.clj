(ns leihs.procurement.resources.room
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(defn room-query
  [id]
  (-> (sql/select :rooms.*)
      (sql/from :rooms)
      (sql/where [:= :rooms.id id])
      sql/format))

(defn get-room-by-id [tx id] (first (jdbc/query tx (room-query id))))

(defn get-room
  [context _ value]
  (get-room-by-id (-> context
                      :request
                      :tx)
                  (:value value) ; for RequestFieldRoom
    ))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
