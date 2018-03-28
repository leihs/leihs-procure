(ns leihs.procurement.resources.category
  (:require [leihs.procurement.resources.sql :as sql]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.db :as db]))

(defn category-query [id]
  (-> (sql/select :*)
      (sql/from :procurement_categories)
      (sql/where [:= :procurement_categories.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-category [id]
  (first (jdbc/query db/conn (category-query id))))

(defn inspectable-by? [user category]
  (:result
    (jdbc/query
      db/conn
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
