(ns leihs.procurement.resources.user
  (:require
    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.procurement.utils.helpers :refer [to-uuid]]
    [honey.sql.helpers :as sql]
    [next.jdbc :as jdbc]
    [taoensso.timbre :refer [debug error info spy warn]]))

(def user-base-query
  (-> (sql/select :id :firstname :lastname)
      (sql/from :users)))

(defn get-user
  [context _ value]
  (jdbc/execute-one! (-> context
                         :request
                         :tx-next)
                     (-> user-base-query
                         (sql/where [:= :users.id (or (:user_id value) ; for
                                         ; RequesterOrganization
                                         (:value value) ; for RequestFieldUser
                                       )])
                         sql-format)))

(defn get-user-by-id
  [tx id]
  (jdbc/execute-one! tx (-> user-base-query
                         (sql/where [:= :id (to-uuid id) ])
                         sql-format)))
