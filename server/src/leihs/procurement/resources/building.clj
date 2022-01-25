(ns leihs.procurement.resources.building
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.resources.buildings :as buildings]
            [leihs.procurement.utils.sql :as sql]))

(defn building-query
  [id]
  (-> (sql/select :buildings.*)
      (sql/from :buildings)
      (sql/where [:= :buildings.id id])
      sql/format))

(defn get-building-by-id
  [tx id]
  (-> id
      building-query
      (->> (jdbc/query tx))
      first))

(defn get-general [tx] (get-building-by-id tx buildings/general-id))

(defn get-building
  [context args value]
  (first (jdbc/query (-> context
                         :request
                         :tx)
                     (building-query (:building_id value)))))

;#### debug ###################################################################


; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
