(ns leihs.procurement.permissions.categories
    (:require
      ;[clojure.java.jdbc :as jdbc]
      ;[leihs.procurement.utils.sql :as sql]

      [honey.sql :refer [format] :rename {format sql-format}]
      [leihs.core.db :as db]
      [next.jdbc :as jdbc]
      [honey.sql.helpers :as sql]

      ))

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
