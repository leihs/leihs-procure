(ns leihs.procurement.resources.rooms
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def rooms-base-query
  (-> (sql/select :rooms.*)
      (sql/from :rooms)))

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

(defn get-building-rooms
  [context _ value]
  (jdbc/query (-> context
                  :request
                  :tx)
              (sql/format (rooms-query (-> value
                                           :building
                                           :id)))))
