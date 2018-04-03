(ns leihs.procurement.resources.category
  (:require [leihs.procurement.utils.sql :as sql]
            [clojure.java.jdbc :as jdbc]))

(defn category-query [id]
  (-> (sql/select :*)
      (sql/from :procurement_categories)
      (sql/where [:= :procurement_categories.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-category [{tx :tx} id]
  (first (jdbc/query tx (category-query id))))

(defn inspectable-by? [{tx :tx} user category]
  (:result
    (jdbc/query
      tx
      (-> (sql/select
            [(sql/call
               :exists
               (-> (sql/select 1)
                   (sql/from :procurement_category_inspectors)
                   (sql/where [:=
                               :procurement_category_inspectors.user_id
                               (:id user)])
                   (sql/merge-where [:=
                                     :procurement_category_inspectors.category_id
                                     (:id category)])))
             :result])
          sql/format))))
