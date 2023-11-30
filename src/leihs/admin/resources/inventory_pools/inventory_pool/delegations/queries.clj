(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.queries
  (:refer-clojure :exclude [str keyword])
  (:require [leihs.core.core :refer [keyword str presence]])
  (:require
   [leihs.admin.utils.regex :refer [uuid-pattern]]

   [leihs.core.sql :as sql]))

(defn member-expr [inventory-pool-id]
  [:exists (-> (sql/select :true)
               (sql/from :access_rights)
               (sql/merge-where [:= :access_rights.inventory_pool_id inventory-pool-id])
               (sql/merge-where [:= :access_rights.user_id :delegations.id]))])

(def contracts-count
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/merge-where [:= :contracts.user_id :delegations.id])))

(defn contracts-count-per-pool [inventory-pool-id]
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/merge-where [:= :contracts.user_id :delegations.id])
      (sql/merge-where [:= :contracts.inventory_pool_id inventory-pool-id])))

(defn contracts-count-open-per-pool [inventory-pool-id]
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/merge-where [:= :contracts.user_id :delegations.id])
      (sql/merge-where [:= :contracts.inventory_pool_id inventory-pool-id])
      (sql/merge-where [:= :contracts.state "open"])))

(def direct-users-count
  (-> (sql/select :%count.*)
      (sql/from :delegations_direct_users)
      (sql/merge-where
       [:= :delegations_direct_users.delegation_id
        :delegations.id])))

(def groups-count
  (-> (sql/select :%count.*)
      (sql/from :delegations_groups)
      (sql/merge-where
       [:= :delegations_groups.delegation_id
        :delegations.id])))

(def pools-count
  (-> (sql/select :%count.*)
      (sql/from :access_rights)
      (sql/merge-where
       [:= :delegations.id :access_rights.user_id])))

(def users-count
  (-> (sql/select :%count.*)
      (sql/from :delegations_users)
      (sql/merge-where
       [:= :delegations_users.delegation_id :delegations.id])))

(def responsible-user
  (-> (sql/select (sql/raw "json_build_object('id', id, 'email', email, 'firstname', firstname, 'lastname', lastname, 'img32_url', img32_url) "))
      (sql/from :users)
      (sql/merge-where [:= :users.id :delegations.delegator_user_id])))

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
