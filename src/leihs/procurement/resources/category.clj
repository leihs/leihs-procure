(ns leihs.procurement.resources.category
  (:require [honeysql.core :as sql]
            [honeysql.helpers :refer :all :rename {update honey-update}]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.db :as db]))

(defn category-query [id]
  (-> (select :*)
      (from :procurement_categories)
      (where [:= :procurement_categories.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-category [id]
  (first (jdbc/query db/conn (category-query id))))

(defn inspectable-by? [user category]
  (:result
    (jdbc/query
      db/conn
      (-> (select
            [(sql/call
               :exists
               (-> (select 1)
                   (from :procurement_category_inspectors)
                   (where [:=
                           :procurement_category_inspectors.user_id
                           (:id user)])
                   (merge-where [:=
                                 :procurement_category_inspectors.category_id
                                 (:id category)])))
             :result])
          sql/format))))
