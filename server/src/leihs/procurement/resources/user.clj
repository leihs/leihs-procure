(ns leihs.procurement.resources.user
  (:require [clojure.java.jdbc :as jdbc]
            [leihs.procurement.utils.sql :as sql]))

(def user-base-query
  (-> (sql/select :users.*)
      (sql/from :users)))

(defn get-user
  [context _ value]
  (first (jdbc/query (-> context
                         :request
                         :tx)
                     (-> user-base-query
                         (sql/where [:= :users.id
                                     (or (:user_id value) ; for
                                         ; RequesterOrganization
                                         (:value value) ; for RequestFieldUser
                                       )])
                         sql/format))))

(defn get-user-by-id
  [tx id]
  (first (jdbc/query tx
                     (-> user-base-query
                         (sql/where [:= :users.id id])
                         sql/format))))
