(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.queries
  (:require [honey.sql.helpers :as sql]))

(defn member-expr [inventory-pool-id]
  [:exists (-> (sql/select :true)
               (sql/from :access_rights)
               (sql/where [:= :access_rights.inventory_pool_id inventory-pool-id])
               (sql/where [:= :access_rights.user_id :delegations.id]))])

(def contracts-count
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :contracts.user_id :delegations.id])))

(defn contracts-count-per-pool [inventory-pool-id]
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :contracts.user_id :delegations.id])
      (sql/where [:= :contracts.inventory_pool_id inventory-pool-id])))

(defn contracts-count-open-per-pool [inventory-pool-id]
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :contracts.user_id :delegations.id])
      (sql/where [:= :contracts.inventory_pool_id inventory-pool-id])
      (sql/where [:= :contracts.state "open"])))

(def direct-users-count
  (-> (sql/select :%count.*)
      (sql/from :delegations_direct_users)
      (sql/where
       [:= :delegations_direct_users.delegation_id
        :delegations.id])))

(def groups-count
  (-> (sql/select :%count.*)
      (sql/from :delegations_groups)
      (sql/where
       [:= :delegations_groups.delegation_id
        :delegations.id])))

(def pools-count
  (-> (sql/select :%count.*)
      (sql/from :access_rights)
      (sql/where
       [:= :delegations.id :access_rights.user_id])))

(def users-count
  (-> (sql/select :%count.*)
      (sql/from :delegations_users)
      (sql/where
       [:= :delegations_users.delegation_id :delegations.id])))

(def responsible-user
  (-> (sql/select [[:raw "json_build_object('id', id, 'email', email, 'firstname', firstname, 'lastname', lastname, 'img32_url', img32_url) "]])
      (sql/from :users)
      (sql/where [:= :users.id :delegations.delegator_user_id])))

(defn find-responsible-user [unique-id]
  (-> (sql/select :*)
      (sql/from :users)
      (sql/where [:= nil :delegator_user_id])
      (sql/where
       [:or
        (when (clojure.string/includes? unique-id "@")
          [:= [:lower :users.email] [:lower unique-id]])
        (when (instance? java.util.UUID unique-id)
          [:= :users.id unique-id])])))
