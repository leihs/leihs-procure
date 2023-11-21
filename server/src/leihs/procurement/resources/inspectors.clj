(ns leihs.procurement.resources.inspectors
  (:require 
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    
            [leihs.procurement.resources.users :refer [users-base-query]]
    ))

(defn get-inspectors
  [context _ value]
  (jdbc/execute! (-> context
                  :request
                  :tx)
              (-> users-base-query
                  (sql/where
                    [:in :users.id
                     (-> (sql/select :pci.user_id)
                         (sql/from [:procurement_category_inspectors :pci])
                         (sql/where [:= :pci.category_id (:id value)]))])
                  sql-format)))

(defn delete-inspectors-for-category-id!
  [tx c-id]
  (jdbc/execute! tx
                 (-> (sql/delete-from [:procurement_category_inspectors :pci])
                     (sql/where [:= :pci.category_id c-id])
                     sql-format)))

(defn insert-inspectors!
  [tx row-maps]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_category_inspectors)
                     (sql/values row-maps)
                     sql-format)))

(defn update-inspectors!
  [tx c-id u-ids]
  (delete-inspectors-for-category-id! tx c-id)
  (if (not (empty? u-ids))
    (insert-inspectors! tx
                        (map #(hash-map :user_id % :category_id c-id) u-ids))))

