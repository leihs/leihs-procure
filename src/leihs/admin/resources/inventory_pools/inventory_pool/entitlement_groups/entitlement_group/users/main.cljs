(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.users.main
  (:require
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.membership.users.main :as membership-users]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.core :as entitlement-group]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as pool-users]
   [leihs.admin.resources.users.main :as users]
   [leihs.core.core :refer [presence]]))

;;; direct member ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn direct-member-path-fn [user]
  (path :inventory-pool-entitlement-group-direct-user
        {:inventory-pool-id @inventory-pool/id*
         :entitlement-group-id @entitlement-group/id*
         :user-id (:id user)}))

;;; group member ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-path-fn
  ([user] (groups-path-fn user {} {}))
  ([user more-route-params more-query-params]
   (path :inventory-pool-entitlement-group-groups
         (merge {:inventory-pool-id @inventory-pool/id*
                 :entitlement-group-id @entitlement-group/id*}
                more-route-params)
         (merge {:including-user (or (-> user :email presence) (:id user))}
                more-query-params))))

;;; rendering ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn users-table-section []
  [:section.entitlement-group-users
   [users/users-table
    [pool-users/user-th-component
     membership-users/member-user-th-component
     membership-users/direct-member-user-th-component
     membership-users/group-member-user-th-component]
    [pool-users/user-td-component
     membership-users/member-user-td-component
     (membership-users/create-direct-member-user-td-component
      direct-member-path-fn)
     (membership-users/create-group-member-user-td-component
      groups-path-fn)]
    :membership-filter? true]])

(defn page []
  [:article.inventory-pool-entitlement-group-users
   [entitlement-group/header]
   [entitlement-group/tabs]
   [membership-users/filter-component]
   [table/toolbar]
   [users-table-section]
   [table/toolbar]
   [users/debug-component]])
