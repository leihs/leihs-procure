(ns leihs.procurement.resources.room
  (:require
    [clj-logging-config.log4j :as logging-config]
    [clojure.java.jdbc :as jdbc]
    [leihs.procurement.utils.sql :as sql]
    [logbug.debug :as debug]
    ))

(defn room-query [id]
  (-> (sql/select :rooms.*)
      (sql/from :rooms)
      (sql/where [:= :rooms.id id])
      sql/format))

(defn get-room [context _ value]
  (first (jdbc/query (-> context :request :tx)
                     (room-query (:room_id value)))))

;#### debug ###################################################################
(logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
