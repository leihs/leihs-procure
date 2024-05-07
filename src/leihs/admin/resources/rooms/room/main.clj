(ns leihs.admin.resources.rooms.room.main
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.admin.resources.rooms.shared :as shared]
   [leihs.core.uuid :refer [uuid]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [insert! query update!] :rename {query jdbc-query,
                                                          insert! jdbc-insert!,
                                                          update! jdbc-update!}]))

;;; room ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn room-query [room-id]
  (-> (apply sql/select shared/default-fields)
      (sql/from :rooms)
      (sql/where [:= :id room-id])))

(defn room [tx room-id]
  (-> room-id
      uuid
      room-query
      (sql/select [:general :is_general])
      sql-format
      (->> (jdbc-query tx))
      first))

(defn get-room
  [{tx :tx {room-id :room-id} :route-params}]
  {:body (room tx room-id)})

;;; delete group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-room
  [{tx :tx {room-id :room-id} :route-params}]
  (assert room-id)
  (let [room (-> (sql/select :*)
                 (sql/from :rooms)
                 (sql/where [:= :id (uuid room-id)])
                 sql-format
                 (->> (jdbc-query tx))
                 first)]
    (when (:general room)
      (throw (ex-info "A general room cannot be deleted." {:status 403}))))
  (if (= (uuid room-id)
         (:id (jdbc/execute-one! tx
                                 ["DELETE FROM rooms WHERE id = ?" (uuid room-id)]
                                 {:return-keys true})))
    {:status 204}
    {:status 404 :body "Delete room failed without error."}))

;;; update room ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-room
  [{{room-id :room-id} :route-params tx :tx data :body :as request}]
  (when (->> ["SELECT true AS exists FROM rooms WHERE id = ?" (uuid room-id)]
             (jdbc-query tx)
             first :exists)
    (jdbc-update! tx :rooms
                  (-> data
                      (select-keys shared/default-fields)
                      (update :building_id uuid)
                      (update :id uuid))
                  ["id = ?" (uuid room-id)])
    {:status 204}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes [request]
  (case (:request-method request)
    :get (get-room request)
    :patch (patch-room request)
    :delete (delete-room request)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
