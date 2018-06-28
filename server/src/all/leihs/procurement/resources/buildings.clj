(ns leihs.procurement.resources.buildings
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def buildings-base-query
  (-> (sql/select :buildings.*)
      (sql/from :buildings)))

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
