(ns leihs.procurement.resources.inspectors
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.core.utils :refer [my-cast]]
    [leihs.procurement.resources.users :refer [users-base-query]]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))

(defn get-inspectors
  [context _ value]
  (jdbc/execute! (-> context
                     :request
                     :tx-next)
                 (-> users-base-query
                     (sql/where [:in :users.id (-> (sql/select :pci.user_id)
                                                   (sql/from [:procurement_category_inspectors :pci])
                                                   (sql/where [:= :pci.category_id [:cast (:id value) :uuid]]))])
                     sql-format)))

(defn delete-inspectors-for-category-id!
  [tx c-id]
  (jdbc/execute! tx (-> (sql/delete-from :procurement_category_inspectors :pci)
                        (sql/where [:= :pci.category_id [:cast (spy c-id) :uuid]])
                        sql-format)))

(defn insert-inspectors!
  [tx row-maps]
  (jdbc/execute! tx (-> (sql/insert-into :procurement_category_inspectors)
                        (sql/values (map #(my-cast %) row-maps))
                        sql-format)))

(defn update-inspectors!
  [tx c-id u-ids]
  (delete-inspectors-for-category-id! tx c-id)
  (if (not (empty? u-ids))
    (insert-inspectors! tx (map #(hash-map :user_id % :category_id c-id) u-ids))))
