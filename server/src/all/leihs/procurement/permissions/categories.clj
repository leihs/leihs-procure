(ns leihs.procurement.permissions.categories
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def categories-base-query
  (-> (sql/select :procurement_categories.*)
      (sql/from :procurement_categories)))

(defn inspected-categories
  [tx user]
  (jdbc/query tx
              (-> categories-base-query
                  (sql/join :procurement_category_inspectors
                            [:= :procurement_categories.id
                             :procurement_category_inspectors.category_id])
                  (sql/merge-where [:= :procurement_category_inspectors.user_id
                                    (:id user)])
                  sql/format)))

(defn viewed-categories
  [tx user]
  (jdbc/query
    tx
    (-> categories-base-query
        (sql/join :procurement_category_viewers
                  [:= :procurement_categories.id
                   :procurement_category_viewers.category_id])
        (sql/merge-where [:= :procurement_category_viewers.user_id (:id user)])
        sql/format)))
