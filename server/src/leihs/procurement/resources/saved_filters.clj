(ns leihs.procurement.resources.saved-filters
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [honey.sql.helpers :as sql]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))

(defn saved-filters-query
  [user-id]
  (spy (-> (sql/select :*)
           (sql/from :procurement_users_filters)
           (sql/where [:= :user_id [:cast user-id :uuid]])
           sql-format)))

(defn get-saved-filters
  [context args value]
  ((jdbc/execute-one! (-> context
                          :request
                          :tx-next)
                      (saved-filters-query (-> value
                                               :user
                                               :id)))))

(defn get-saved-filters-by-user-id
  [tx user-id]
  (jdbc/execute-one! tx (saved-filters-query user-id)))

(defn delete-unused
  [tx]
  (jdbc/execute!
    tx
    (-> (sql/delete-from :procurement_users_filters :puf)
        (sql/where [:not [:exists (-> (sql/select true)
                                      (sql/from [:procurement_requesters_organizations :pro])
                                      (sql/where [:= :pro.user_id :puf.user_id]))]])
        sql-format)))

