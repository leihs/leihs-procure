(ns leihs.procurement.resources.building
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.procurement.resources.buildings :as buildings]
   [next.jdbc :as jdbc]))

(defn building-query
  [id]
  (-> (sql/select :buildings.*)
      (sql/from :buildings)
      (sql/where [:= :buildings.id id])
      sql-format))

(defn get-building-by-id
  [tx id]
  (-> id
      building-query
      (->> (jdbc/execute-one! tx))))

(defn get-general [tx] (get-building-by-id tx buildings/general-id))

(defn get-building
  [context args value]
  (jdbc/execute-one! (-> context
                         :request
                         :tx-next)
                     (building-query (:building_id value))))

;#### debug ###################################################################

; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
; (debug/undebug-ns *ns*)
