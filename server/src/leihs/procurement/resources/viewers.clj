(ns leihs.procurement.resources.viewers
  (:require
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
     
            [clojure.tools.logging :as log]
            [leihs.procurement.resources.users :refer [users-base-query]]
    ))

(defn get-viewers
  [context _ value]
  (jdbc/execute! (-> context
                  :request
                  :tx-next)
              (-> users-base-query
                  (sql/where
                    [:in :users.id
                     (-> (sql/select :pcv.user_id)
                         (sql/from [:procurement_category_viewers :pcv])
                         (sql/where [:= :pcv.category_id [:cast (:id value):uuid]]))])
                  sql-format)))

(defn delete-viewers-for-category-id!
  [tx c-id]
  (jdbc/execute! tx
                 (-> (sql/delete-from [:procurement_category_viewers :pcv])
                     (sql/where [:= :pcv.category_id [:cast c-id :uuid]])
                     sql-format)))

(defn insert-viewers!
  [tx row-maps]
  (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_category_viewers)
                     (sql/values row-maps)
                     sql-format)))

(defn update-viewers!
  [tx c-id u-ids]
  (delete-viewers-for-category-id! tx c-id)
  (if (not (empty? u-ids))
    (insert-viewers! tx (map #(hash-map :user_id % :category_id c-id) u-ids))))
