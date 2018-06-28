(ns leihs.procurement.resources.attachments
  (:require [clj-logging-config.log4j :as logging-config]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.utils.sql :as sql]
            [logbug.debug :as debug]))

(def attachments-base-query
  (-> (sql/select :procurement_attachments.*)
      (sql/from :procurement_attachments)))

(defn get-attachments-for-request-id
  [tx request-id]
  (let [query (-> attachments-base-query
                  (sql/merge-where [:= :procurement_attachments.request_id
                                    request-id])
                  sql/format)]
    (->> query
         (jdbc/query tx)
         (map #(merge % {:url (path :attachment {:attachment-id (:id %)})})))))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
