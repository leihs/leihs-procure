(ns leihs.procurement.resources.room
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

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

