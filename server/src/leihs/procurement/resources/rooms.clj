(ns leihs.procurement.resources.rooms
  (:require [honey.sql :refer [format] :rename {format sql-format}]
            [honey.sql.helpers :as sql]
            [leihs.procurement.resources.building :as building]
            [leihs.procurement.resources.buildings :as buildings]
            [next.jdbc :as jdbc]))

(def rooms-base-query
  (-> (sql/select :rooms.*)
      (sql/from :rooms)
      (sql/order-by [:general :desc] [:name :asc])))

(defn general-from-general
  [tx]
  (-> rooms-base-query
      (sql/where [:= :rooms.general true])
      (sql/where [:= :rooms.building_id buildings/general-id])
      sql-format
      (->> (jdbc/execute-one! tx))
      (assoc :building (building/get-general tx))))

(defn rooms-query
  [args value]
  (let [building_id (or (:building_id args) (:id value))]
    (cond-> rooms-base-query
      building_id (sql/where [:= :rooms.building_id building_id]))))

(defn get-rooms
  [context args value]
  (jdbc/execute! (-> context
                     :request
                     :tx-next)
                 (sql-format (rooms-query args value))))
