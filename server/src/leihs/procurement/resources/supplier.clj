(ns leihs.procurement.resources.supplier
  (:require [clojure.java.jdbc :as jdbc]
                [taoensso.timbre :refer [debug info warn error spy]]


            [leihs.procurement.utils.sql :as sql]))

(defn supplier-query
  [id]
  (println ">o> tocheck suppliers.id=" id)
  (spy (-> (sql/select :suppliers.*)
      (sql/from :suppliers)
      (sql/where [:= :suppliers.id [:cast (spy id) :uuid]])
      ;(sql/where [:= :suppliers.id (spy id) ])
      sql/format)))

(defn get-supplier-by-id [tx id] (first (jdbc/query tx (supplier-query id))))

(defn get-supplier
  [context _ value]
  (get-supplier-by-id (-> context
                          :request
                          :tx)
                      (or (:value (spy value)) ; for RequestFieldSupplier
                          (:supplier_id (spy value)))))
