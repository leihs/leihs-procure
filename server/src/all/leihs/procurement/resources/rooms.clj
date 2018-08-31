(ns leihs.procurement.resources.rooms
  (:require [clojure.tools.logging :as log]
            [leihs.procurement.resources.buildings :as buildings]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def rooms-base-query
  (-> (sql/select :rooms.*)
      (sql/from :rooms)
      (sql/order-by [:general :desc] [:name :asc])))

(defn general-from-general
  [tx]
  (-> rooms-base-query
      (sql/merge-where [:= :rooms.general true])
      (sql/merge-where [:= :rooms.building_id buildings/general-id])
      sql/format
      (->> (jdbc/query tx))
      first))

(defn rooms-query
  [args value]
  (let [building_id (or (:building_id args) (:id value))]
    (cond-> rooms-base-query
      building_id (sql/merge-where [:= :rooms.building_id building_id]))))

(defn get-rooms
  [context args value]
  (jdbc/query (-> context
                  :request
                  :tx)
              (sql/format (rooms-query args value))))
