(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.queries
  (:require
   [honey.sql.helpers :as sql]))

(def contracts-count
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :contracts.inventory_pool_id :inventory-pools.id])
      (sql/where [:= :contracts.user_id :users.id])))

(def open-contracts-count
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :contracts.user_id :users.id])
      (sql/where [:= :contracts.inventory_pool_id :inventory-pools.id])
      (sql/where [:= :contracts.state "open"])))

(def closed-contracts-count
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :contracts.user_id :users.id])
      (sql/where [:= :contracts.inventory_pool_id :inventory-pools.id])
      (sql/where [:= :contracts.state "closed"])))

(defn contracts-count-per-pool [inventory-pool-id]
  (-> (sql/select :%count.*)
      (sql/from :contracts)
      (sql/where [:= :contracts.user_id :users.id])
      (sql/where [:= :contracts.inventory_pool_id inventory-pool-id])))

(def direct-users-count
  (-> (sql/select :%count.*)
      (sql/from :users_direct_users)
      (sql/where [:= :users_direct_users.delegation_id :users.id])))

(def groups-count
  (-> (sql/select :%count.*)
      (sql/from :users_groups)
      (sql/where [:= :users_groups.delegation_id :users.id])))

(def pools-count
  (-> (sql/select :%count.*)
      (sql/from :access_rights)
      (sql/where [:= :users.id :access_rights.user_id])))

(def users-count
  (-> (sql/select :%count.*)
      (sql/from :users_users)
      (sql/where [:= :users_users.delegation_id :users.id])))

(def responsible-user
  (-> (sql/select [[:raw "json_build_object('id', id, 'email', email, 'firstname', firstname, 'lastname', lastname, 'img32_url', img32_url) "]])
      (sql/from :users)
      (sql/where [:= :users.id :users.delegator_user_id])))

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
