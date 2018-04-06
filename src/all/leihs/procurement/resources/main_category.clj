(ns leihs.procurement.resources.main-category
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]
            ))

(defn main-category-query [id]
  (-> (sql/select :procurement_main_categories.*)
      (sql/from :procurement_main_categories)
      (sql/where [:= :procurement_main_categories.id id])
      sql/format))

(defn get-main-category [context _ value]
  (first (jdbc/query (-> context :request :tx)
                     (main-category-query (:main_category_id value)))))
