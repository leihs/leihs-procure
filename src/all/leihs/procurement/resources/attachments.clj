(ns leihs.procurement.resources.attachments
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(def attachments-base-query
  (-> (sql/select :procurement_attachments.*)
      (sql/from :procurement_attachments)))

(defn get-attachments [context _ value]
  (jdbc/query (-> context :request :tx)
              (sql/format
                (-> attachments-base-query
                    (sql/merge-where [:=
                                      :procurement_attachments.request_id
                                      (:id value)])))))

;#### debug ###################################################################
(logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
