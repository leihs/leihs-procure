(ns leihs.procurement.resources.user
  (:require [honeysql.core :as sql]
            [honeysql.helpers :refer :all :rename {update honey-update}]
            [clojure.java.jdbc :as jdbc]
            [leihs.procurement.db :as db]))

(def user-id "c0777d74-668b-5e01-abb5-f8277baa0ea8")

(defn user-query [id]
  (-> (select :*)
      (from :users)
      (where [:= :users.id (sql/call :cast id :uuid)])
      sql/format))

(defn get-user [id]
  (first (jdbc/query db/conn (user-query id))))

(defn procurement-requester? [user]
  (:is_procurement_requester user))

(defn procurement-admin? [user]
  (:is_procurement_admin user))

(defn procurement-inspector? [user]
  (:result
    (jdbc/query
      db/conn
      (-> (select
            [(sql/call
               :exists
               (-> (select 1)
                   (from :procurement_category_inspectors)
                   (where [:=
                           :procurement_category_inspectors.user_id
                           (:id user)])))
             :result])
          sql/format))))

; (procurement-inspector? (get-user user-id))
