(ns leihs.procurement.resources.supplier
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))

(defn supplier-query
  [id]
  (-> (sql/select :suppliers.*)
      (sql/from :suppliers)
      (sql/where [:= :suppliers.id [:cast (spy id) :uuid]])
      sql-format))

(defn get-supplier-by-id [tx id] (jdbc/execute-one! tx (supplier-query id)))

(defn get-supplier
  [context _ value]
  (get-supplier-by-id (-> context
                          :request
                          :tx-next)
                      (or (:value value)                     ; for RequestFieldSupplier
                          (:supplier_id value))))
