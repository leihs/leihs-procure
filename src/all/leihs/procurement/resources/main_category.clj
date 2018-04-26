(ns leihs.procurement.resources.main-category
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]
            ))

(def main-category-base-query
 (-> (sql/select :procurement_main_categories.*)
     (sql/from :procurement_main_categories))) 

(defn main-category-query-by-id [id]
  (-> main-category-base-query
      (sql/where [:= :procurement_main_categories.id id])
      sql/format))

(defn main-category-query-by-name [mc-name]
  (-> main-category-base-query
      (sql/where [:= :procurement_main_categories.name mc-name])
      sql/format))

(defn get-main-category [context _ value]
  (first (jdbc/query (-> context :request :tx)
                     (main-category-query-by-id (:main_category_id value)))))

(defn get-main-category-by-name [tx mc-name]
  (first (jdbc/query tx (main-category-query-by-name mc-name))))

(defn insert-main-category! [tx mc]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_main_categories)
                     (sql/values [mc])
                     sql/format)))
