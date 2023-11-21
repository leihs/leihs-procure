(ns leihs.procurement.resources.supplier
  (:require 
    
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    
    ))

(defn supplier-query
  [id]
  (-> (sql/select :suppliers.*)
      (sql/from :suppliers)
      (sql/where [:= :suppliers.id id])
      sql-format))

(defn get-supplier-by-id [tx id] ( (jdbc/execute-one! tx (supplier-query id))))

(defn get-supplier
  [context _ value]
  (get-supplier-by-id (-> context
                          :request
                          :tx)
                      (or (:value value) ; for RequestFieldSupplier
                          (:supplier_id value))))
