(ns leihs.procurement.resources.buildings
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def general-id "abae04c5-d767-425e-acc2-7ce04df645d1")

(def buildings-base-query
  (-> (sql/select :buildings.*)
      (sql/from :buildings)
      (sql/order-by [(sql/call := general-id :id) :desc] [:name :asc])))

(defn buildings-query
  [args]
  (let [id (:id args)]
    (cond-> buildings-base-query id (sql/merge-where [:= :buildings.id id]))))

(defn get-buildings
  [context args _]
  (jdbc/query (-> context
                  :request
                  :tx)
              (sql/format (buildings-query args))))
