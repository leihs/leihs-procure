(ns procurement-graphql.resources.user
  (:require [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [clojure.java.jdbc :as jdbc]
            [procurement-graphql.db :as db]))

(def user-id "c0777d74-668b-5e01-abb5-f8277baa0ea8")

(defn user-query [id]
  (-> (select :*)
      (from :users)
      (where [:= :users.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-user [id]
  (first (jdbc/query db/db (user-query id))))

(defn procurement-requester? [id]
  (:is_procurement_requester (get-user id)))

; (procurement-requester? user-id)

; (in-ns 'procurement-graphql.resources.user)

; (require '([procurement-graphql.resources.user :reload-all true]))
