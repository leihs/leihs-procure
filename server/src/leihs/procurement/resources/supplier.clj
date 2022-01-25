(ns leihs.procurement.resources.supplier
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(defn supplier-query
  [id]
  (-> (sql/select :suppliers.*)
      (sql/from :suppliers)
      (sql/where [:= :suppliers.id id])
      sql/format))

(defn get-supplier-by-id [tx id] (first (jdbc/query tx (supplier-query id))))

(defn get-supplier
  [context _ value]
  (get-supplier-by-id (-> context
                          :request
                          :tx)
                      (or (:value value) ; for RequestFieldSupplier
                          (:supplier_id value))))
