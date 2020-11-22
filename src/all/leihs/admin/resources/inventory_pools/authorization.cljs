(ns leihs.admin.resources.inventory-pools.authorization
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.user.front :as current-user]

    [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles :refer [expand-role-to-hierarchy]]

    [taoensso.timbre :as logging]))

(defn some-lending-manager? [current-user-state routing-state]
  (if (some
        (fn [ac] (some #(= :lending_manager)
                       (-> ac :role roles/expand-role-to-hierarchy)))
        (:access-rights current-user-state))
    true
    false))


(defn roles-in-pool [user-state inventory-pool-id]
  (set (some->> user-state
                :access-rights
                (some #(when (= inventory-pool-id (:inventory_pool_id %))
                         (:role %)))
                expand-role-to-hierarchy)))

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
;(logging-config/set-logger! :level :debug)
;(debug/wrap-with-log-debug #'activity-filter)
;(debug/wrap-with-log-debug #'set-order)
;(debug/wrap-with-log-debug #'inventory-pools-query)
;(debug/wrap-with-log-debug #'inventory-pools-formated-query)
;(logging-config/set-logger! :level :info)
;(debug/debug-ns *ns*)

;(-> *request* :route-params :inventory-pool-id)

;(pool-access-right-for-route *request*)
;(pool-lending-manager? *request*)
