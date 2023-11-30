(ns leihs.admin.resources.inventory-pools.inventory-pool.users.queries
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
   [leihs.admin.utils.regex :refer [uuid-pattern]]

   [leihs.core.sql :as sql]))

(def contracts-count
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/merge-where [:= :contracts.user_id :users.id])))

(defn contracts-count-per-pool [inventory-pool-id]
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/merge-where [:= :contracts.user_id :users.id])
      (sql/merge-where [:= :contracts.inventory_pool_id inventory-pool-id])))

(defn contracts-count-open-per-pool [inventory-pool-id]
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/merge-where [:= :contracts.user_id :users.id])
      (sql/merge-where [:= :contracts.inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :contracts.state "open"])))

(def direct-users-count
  (-> (sql/select :%count.*)
      (sql/from :users_direct_users)
      (sql/merge-where
       [:= :users_direct_users.delegation_id
        :users.id])))

(def groups-count
  (-> (sql/select :%count.*)
      (sql/from :users_groups)
      (sql/merge-where
       [:= :users_groups.delegation_id
        :users.id])))

(def pools-count
  (-> (sql/select :%count.*)
      (sql/from :access_rights)
      (sql/merge-where
       [:= :users.id :access_rights.user_id])))

(def users-count
  (-> (sql/select :%count.*)
      (sql/from :users_users)
      (sql/merge-where
       [:= :users_users.delegation_id :users.id])))

(def responsible-user
  (-> (sql/select (sql/raw "json_build_object('id', id, 'email', email, 'firstname', firstname, 'lastname', lastname, 'img32_url', img32_url) "))
      (sql/from :users)
      (sql/merge-where [:= :users.id :users.delegator_user_id])))

(defn find-responsible-user [unique-id]
  (-> (sql/select :*)
      (sql/from :users)
      (sql/merge-where [:= nil :delegator_user_id])
      (sql/merge-where
       [:or
        (when (clojure.string/includes? unique-id "@")
          [:= (sql/call :lower :users.email) (sql/call :lower unique-id)])
        (when (re-matches uuid-pattern unique-id)
          [:= :users.id unique-id])])))
