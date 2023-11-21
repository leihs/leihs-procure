(ns leihs.procurement.resources.buildings
  (:require 
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    
    ))

(def general-id "abae04c5-d767-425e-acc2-7ce04df645d1")

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
                  :tx)
              (sql-format (buildings-query args))))
