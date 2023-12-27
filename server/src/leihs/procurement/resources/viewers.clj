(ns leihs.procurement.resources.viewers
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [leihs.core.utils :refer [my-cast]]
    [leihs.procurement.resources.users :refer [users-base-query]]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))

(defn get-viewers
  [context _ value]
  (jdbc/execute! (-> context
                     :request
                     :tx-next)
                 (-> users-base-query (sql/where [:in :users.id
                                                  (-> (sql/select :pcv.user_id)
                                                      (sql/from [:procurement_category_viewers :pcv])
                                                      (sql/where [:= :pcv.category_id [:cast (:id value) :uuid]]))])
                     sql-format)))

(defn delete-viewers-for-category-id!
  [tx c-id]
  (let [result (spy (jdbc/execute-one! tx (-> (sql/delete-from :procurement_category_viewers :pcv)
                                              (sql/where [:= :pcv.category_id [:cast c-id :uuid]])
                                              sql-format)))

        res (spy (:update-count result))                    ;; TODO: fixme
        result (spy (:next.jdbc/update-count result))]
    (spy (list result))))

(defn insert-viewers!
  [tx row-maps]
  (-> (jdbc/execute-one! tx (-> (sql/insert-into :procurement_category_viewers)
                                (sql/values (map #(my-cast %) row-maps))
                                sql-format))
      :next.jdbc/update-count
      list))

(defn update-viewers!
  [tx c-id u-ids]
  (-> (delete-viewers-for-category-id! tx c-id))
  (if (not (empty? u-ids))
    (insert-viewers! tx (map #(hash-map :user_id % :category_id c-id) u-ids))))
