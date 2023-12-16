(ns leihs.procurement.resources.viewers
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]

                [taoensso.timbre :refer [debug info warn error spy]]


            [leihs.procurement.resources.users :refer [users-base-query]]
            [leihs.procurement.utils.sql :as sql]))

(defn get-viewers
  [context _ value]
  (jdbc/query (-> context
                  :request
                  :tx)
              (-> users-base-query
                  (sql/merge-where
                    [:in :users.id
                     (-> (sql/select :pcv.user_id)
                         (sql/from [:procurement_category_viewers :pcv])
                         (sql/merge-where [:= :pcv.category_id (:id value)]))])
                  sql/format)))

(defn delete-viewers-for-category-id!
  [tx c-id]
  (jdbc/execute! tx
                 (-> (sql/delete-from [:procurement_category_viewers :pcv])
                     (sql/merge-where [:= :pcv.category_id c-id])
                     sql/format)))

(defn insert-viewers!
  [tx row-maps]
  (spy (jdbc/execute! tx
                 (-> (sql/insert-into :procurement_category_viewers)
                     (sql/values row-maps)
                     sql/format))))

(defn update-viewers!
  [tx c-id u-ids]
  (spy (delete-viewers-for-category-id! tx c-id))
  (if (spy (not (empty? u-ids)))
    (spy (insert-viewers! tx (map #(hash-map :user_id % :category_id c-id) u-ids)))))
