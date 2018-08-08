(ns leihs.procurement.resources.attachments
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.paths :refer [path]]
            [leihs.procurement.resources [attachment :as attachment]
             [upload :as upload]]
            [leihs.procurement.utils.sql :as sql]))

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

(defn get-attachments
  [context _ value]
  (let [tx (-> context
               :request
               :tx)]
    (get-attachments-for-request-id tx (:request-id value))))

(defn create-for-request-id-and-uploads!
  [tx req-id uploads]
  (doseq [{u-id :id} uploads]
    (let [u-row (upload/get-by-id tx u-id)
          md (-> u-row
                 :metadata
                 to-json
                 (#(sql/call :cast % :json)))]
      (attachment/create! tx
                          (-> u-row
                              (dissoc :id)
                              (dissoc :created_at)
                              (assoc :metadata md)
                              (assoc :request_id req-id)))
      (upload/delete! tx u-id))))

(defn delete!
  [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_attachments)
                     (sql/where [:in :procurement_attachments.id ids])
                     sql/format)))

;#### debug ###################################################################
; (logging-config/set-logger! :level :debug)
; (logging-config/set-logger! :level :info)
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
