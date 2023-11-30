(ns leihs.admin.resources.inventory-pools.authorization
  (:refer-clojure :exclude [str keyword])
  (:require
   [leihs.admin.common.roles.core :as roles :refer [expand-to-hierarchy]]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.user.front :as current-user]
   [taoensso.timbre]))

(defn some-lending-manager? [current-user-state routing-state]
  (if (some
       (fn [ac] (some #(= :lending_manager)
                      (-> ac :role roles/expand-to-hierarchy)))
       (:access-rights current-user-state))
    true
    false))

(defn roles-in-pool [user-state inventory-pool-id]
  (set (some->> user-state
                :access-rights
                (some #(when (= inventory-pool-id (:inventory_pool_id %))
                         (:role %)))
                expand-to-hierarchy)))

(defn current-user-roles-in-pool [inventory-pool-id]
  (roles-in-pool @current-user/state* inventory-pool-id))

(defn current-user-is-some-manager-of-pool? [inventory-pool-id]
  (boolean (:lending_manager
            (current-user-roles-in-pool inventory-pool-id))))

(defn pool-inventory-manager? [current-user-state routing-state]
  (let [inventory-pool-id (-> routing-state :route-params :inventory-pool-id)]
    (assert inventory-pool-id)
    (let [roles (roles-in-pool @current-user/state* inventory-pool-id)]
      (boolean (:inventory_manager roles)))))

(defn pool-lending-manager?  [current-user-state routing-state]
  (let [inventory-pool-id (-> routing-state :route-params :inventory-pool-id)]
    (assert inventory-pool-id)
    (let [roles (roles-in-pool @current-user/state* inventory-pool-id)]
      (boolean (:lending_manager roles)))))

;#### debug ###################################################################
