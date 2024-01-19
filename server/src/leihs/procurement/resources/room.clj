(ns leihs.procurement.resources.room
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [next.jdbc :as jdbc]))

(defn room-query
  [id]
  (-> (sql/select :rooms.*)
      (sql/from :rooms)
      (sql/where [:= :rooms.id id])
      sql-format))

(defn get-room-by-id [tx id] ((jdbc/execute-one! tx (room-query id))))

(defn get-room
  [context _ value]
  (get-room-by-id (-> context
                      :request
                      :tx-next)
                  (:value value))) ; for RequestFieldRoom
