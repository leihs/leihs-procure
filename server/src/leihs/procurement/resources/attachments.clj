(ns leihs.procurement.resources.attachments
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]
            [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.procurement.paths :refer [path]]
            (leihs.procurement.resources [attachment :as attachment]
                                         [upload :as upload])
            [leihs.procurement.utils.helpers :refer [cast-to-json]]
            [logbug.debug :as debug]

            [next.jdbc :as jdbc]
            [taoensso.timbre :refer [ error info spy warn]]))

(def attachments-base-query
  (-> (sql/select :procurement_attachments.*)
      (sql/from :procurement_attachments)))

(defn get-attachments-for-request-id
  [tx request-id]

  (println ">o> get-attachments-for-request-id" get-attachments-for-request-id)
  (let [query (-> attachments-base-query
                  (sql/where [:= :procurement_attachments.request_id
                              request-id])
                  sql-format
                  spy
                  )]
    (->> query
         (jdbc/execute! tx)
         (map #(merge % {:url (path :attachment {:attachment-id (:id %)})})))))

(defn get-attachments
  [context _ value]

  (println ">o> get-attachments value=" value)
  (let [tx (-> context
               :request
               :tx-next)]
    (get-attachments-for-request-id tx (:request-id value))))

(defn create-for-request-id-and-uploads!
  [tx req-id uploads]
  (println ">o> create-for-request-id-and-uploads! uploads=" uploads)
  (doseq [{u-id :id} uploads]
    (let [u-row (upload/get-by-id tx u-id)
          md (-> u-row
                 :metadata
                 to-json
                 cast-to-json)]
      (attachment/create! tx
                          (-> u-row
                              (dissoc :id)
                              (dissoc :created_at)
                              (assoc :metadata md)
                              (assoc :request_id req-id)))
      (upload/delete! tx u-id))))

(defn delete!
  [tx ids]
  (println ">o> delete!")
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_attachments)
                     (sql/where [:in :procurement_attachments.id ids])
                     sql-format)))


(debug/debug-ns *ns*)