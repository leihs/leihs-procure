(ns leihs.procurement.resources.viewers
  (:require
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.procurement.resources.users :refer [users-base-query]]
   [next.jdbc :as jdbc]
   [taoensso.timbre :refer [debug error info spy warn]]))

(defn get-viewers
  [context _ value]
  (jdbc/execute! (-> context
                     :request
                     :tx)
                 (-> users-base-query (sql/where [:in :users.id
                                                  (-> (sql/select :pcv.user_id)
                                                      (sql/from [:procurement_category_viewers :pcv])
                                                      (sql/where [:= :pcv.category_id (:id value)]))])
                     sql-format)))

(defn delete-viewers-for-category-id!
  [tx c-id]
  (jdbc/execute-one! tx (-> (sql/delete-from :procurement_category_viewers :pcv)
                            (sql/where [:= :pcv.category_id c-id])
                            sql-format)))

(defn insert-viewers!
  [tx row-maps]
  (jdbc/execute-one! tx
                     (-> (sql/insert-into :procurement_category_viewers)
                         (sql/values row-maps)
                         sql-format)))

(defn update-viewers!
  [tx c-id u-ids]
  (-> (delete-viewers-for-category-id! tx c-id))
  (if (not (empty? u-ids))
    (insert-viewers! tx (map #(hash-map :user_id % :category_id c-id) u-ids))))
