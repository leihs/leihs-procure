(ns leihs.procurement.resources.category
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]
            ))

(defn category-query [id]
  (-> (sql/select :*)
      (sql/from :procurement_categories)
      (sql/where [:= :procurement_categories.id id])
      sql/format))

(defn get-category [context _ value]
  (first (jdbc/query (-> context :request :tx)
                     (category-query (:category_id value)))))

(defn get-category-by-id [tx id]
  (first (jdbc/query tx (category-query id))))

(defn inspectable-by? [tx user category]
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
