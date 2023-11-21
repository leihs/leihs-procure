(ns leihs.procurement.resources.buildings
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [next.jdbc :as jdbc]))

(def general-id #uuid "abae04c5-d767-425e-acc2-7ce04df645d1")

(def buildings-base-query
  (-> (sql/select :buildings.*)
      (sql/from :buildings)
      (sql/order-by [(:= general-id :id) :desc] [:name :asc])))

(defn buildings-query
  [args]
  (let [id (:id args)]
    (cond-> buildings-base-query id (sql/where [:= :buildings.id id]))))

(defn get-buildings
  [context args _]
  (jdbc/execute! (-> context
                  :request
                  :tx-next)
              (sql-format (buildings-query args))))
