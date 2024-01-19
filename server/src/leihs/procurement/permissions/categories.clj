(ns leihs.procurement.permissions.categories
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [next.jdbc :as jdbc]))

(def categories-base-query
  (-> (sql/select :procurement_categories.*)
      (sql/from :procurement_categories)))

(defn inspected-categories
  [tx user]
  (jdbc/execute! tx
              (-> categories-base-query
                  (sql/join :procurement_category_inspectors
                            [:= :procurement_categories.id
                             :procurement_category_inspectors.category_id])
                  (sql/where [:= :procurement_category_inspectors.user_id
                                    (:id user)])
                  sql-format)))

(defn viewed-categories
  [tx user]
  (jdbc/execute!
    tx
    (-> categories-base-query
        (sql/join :procurement_category_viewers
                  [:= :procurement_categories.id
                   :procurement_category_viewers.category_id])
        (sql/where [:= :procurement_category_viewers.user_id (:id user)])
        sql-format)))
