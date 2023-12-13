(ns leihs.procurement.resources.attachments
  (:require [cheshire.core :refer [generate-string] :rename
             {generate-string to-json}]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]

        [taoensso.timbre :refer [debug info warn error spy]]


            ;[clojure.java.jdbc :as jdbc]
            ;[leihs.procurement.utils.sql :as sql]

    [leihs.procurement.paths :refer [path]]
            [leihs.procurement.resources [attachment :as attachment]
             [upload :as upload]]
    ))

(def attachments-base-query
  (-> (sql/select :procurement_attachments.*)
      (sql/from :procurement_attachments)))

(defn get-attachments-for-request-id
  [tx request-id]
  (let [query (-> attachments-base-query
                  (sql/where [:= :procurement_attachments.request_id
                                    request-id])
                  sql-format)]
    (->> query
         (jdbc/execute! tx)
         (map #(merge % {:url (path :attachment {:attachment-id (:id %)})})))))

(defn get-attachments
  [context _ value]
  (let [tx (-> context
               :request
               :tx-next)]
    (get-attachments-for-request-id tx (:request-id value))))


;cast-to-json
(defn cast-to-json [comment] [:cast comment :json])

(defn create-for-request-id-and-uploads!
  [tx req-id uploads]
  (doseq [{u-id :id} uploads]
    (let [u-row (upload/get-by-id tx u-id)
          md (-> u-row
                 :metadata
                 to-json

                 cast-to-json
                 ;(#(:cast % :json))


                 )]
      (attachment/create! tx
                          (-> u-row
                              (dissoc :id)
                              (dissoc :created_at)
                              (assoc :metadata md)
                              (assoc :request_id req-id)))
      (upload/delete! tx u-id))))

(defn cast-uuids [uuids]
  (map (fn [uuid-str] [:cast uuid-str :uuid]) uuids))

(defn delete!
  [tx ids]
  (jdbc/execute! tx
                 (-> (sql/delete-from :procurement_attachments)
                     (sql/where [:in :procurement_attachments.id (cast-uuids (spy ids))])
                     sql-format)))
