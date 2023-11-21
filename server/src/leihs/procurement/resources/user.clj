(ns leihs.procurement.resources.user
  (:require 
    ;[clojure.java.jdbc :as jdbc]
    ;        [leihs.procurement.utils.sql :as sql]

    [honey.sql :refer [format] :rename {format sql-format}]
    [leihs.core.db :as db]
    [next.jdbc :as jdbc]
    [honey.sql.helpers :as sql]
    ))

(def user-base-query
  (-> (sql/select :users.id :users.firstname :users.lastname)
      (sql/from :users)))

(defn get-user
  [context _ value]
  ( (jdbc/execute-one! (-> context
                         :request
                         :tx)
                     (-> user-base-query
                         (sql/where [:= :users.id
                                     (or (:user_id value) ; for
                                         ; RequesterOrganization
                                         (:value value) ; for RequestFieldUser
                                       )])
                         sql-format))))

(defn get-user-by-id
  [tx id]
  ( (jdbc/execute-one! tx
                     (-> user-base-query
                         (sql/where [:= :users.id id])
                         sql-format))))
