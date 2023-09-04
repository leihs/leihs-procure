(ns leihs.admin.resources.rooms.room.main
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
    [clojure.set :refer [rename-keys]]
    [compojure.core :as cpj]
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.admin.paths :refer [path]]
    [leihs.admin.resources.rooms.shared :as shared]
    [leihs.core.auth.core :as auth]
    [leihs.core.uuid :refer [uuid]]
    [logbug.catcher :as catcher]
    [logbug.debug :as debug]
    [next.jdbc :as jdbc]
    [next.jdbc.sql :refer [query update! delete! insert!] :rename {query jdbc-query update! jdbc-update! delete! jdbc-delete! insert! jdbc-insert!}]
    [taoensso.timbre :refer [error warn info debug spy]]
    ))

;;; room ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn room-query [room-id]
  (-> (apply sql/select shared/default-fields)
      (sql/from :rooms)
      (sql/where [:= :id room-id])))

(defn room [tx-next room-id]
  (-> room-id
      uuid
      room-query
      (sql/select [:general :is_general])
      sql-format
      (->> (jdbc-query tx-next))
      first))

(defn get-room
  [{tx-next :tx-next {room-id :room-id} :route-params}]
  {:body (room tx-next room-id)})

;;; delete group ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-room
  [{tx-next :tx-next {room-id :room-id} :route-params}]
  (assert room-id)
  (let [room (-> (sql/select :*)
                 (sql/from :rooms)
                 (sql/where [:= :id (uuid room-id)])
                 sql-format
                 (->> (jdbc-query tx-next))
                 first)]
    (when (:general room)
      (throw (ex-info "A general room cannot be deleted." {:status 403}))))
  (if (= (uuid room-id)
         (:id (jdbc/execute-one! tx-next
                                 ["DELETE FROM rooms WHERE id = ?" (uuid room-id)]
                                 {:return-keys true})))
    {:status 204}
    {:status 404 :body "Delete room failed without error."}))

;;; update room ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-room
  [{{room-id :room-id} :route-params tx-next :tx-next data :body :as request}]
  (when (->> ["SELECT true AS exists FROM rooms WHERE id = ?" (uuid room-id)]
             (jdbc-query tx-next)
             first :exists)
    (jdbc-update! tx-next :rooms
                  (-> data
                      (select-keys shared/default-fields)
                      (update :building_id uuid)
                      (update :id uuid))
                  ["id = ?" (uuid room-id)])
    {:status 204}))

;;; routes and paths ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def room-path (path :room {:room-id ":room-id"}))

(def routes
  (cpj/routes
    (cpj/GET room-path [] #'get-room)
    (cpj/PATCH room-path [] #'patch-room)
    (cpj/DELETE room-path [] #'delete-room)))

;#### debug ###################################################################

;(debug/wrap-with-log-debug #'data-url-img->buffered-image)
;(debug/wrap-with-log-debug #'buffered-image->data-url-img)
;(debug/wrap-with-log-debug #'resized-img)

;(debug/debug-ns *ns*)
