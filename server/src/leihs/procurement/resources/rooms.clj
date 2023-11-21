(ns leihs.procurement.resources.rooms
  (:require [clojure.tools.logging :as log]
            [leihs.procurement.resources.building :as building]
            [leihs.procurement.resources.buildings :as buildings]
    
            ;[clojure.java.jdbc :as jdbc]
            ;[leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    ))

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
                  :tx)
              (sql-format (rooms-query args value))))
