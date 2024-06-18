(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.users.main
  (:require
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.membership.users.main :as users-membership]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as delegation]
   [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as pool-users]
   [leihs.admin.resources.users.main :as users]
   [leihs.admin.state :as state]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]))

;;; path helpers  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn direct-member-path [user]
  (path :inventory-pool-delegation-user
        {:inventory-pool-id @inventory-pool/id*
         :delegation-id @delegation/id* :user-id (:id user)}))

(defn groups-path-fn
  ([user] (groups-path-fn user {} {}))
  ([user more-route-params more-query-params]
   (path :inventory-pool-delegation-groups
         (merge
          {:inventory-pool-id @inventory-pool/id*
           :delegation-id @delegation/id*
           :user-id (:id user)}
          more-route-params)

         (merge
          {:including-user (or (-> user :email presence) (:id user))}
          more-query-params))))

;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div]))

(defn table []
  [users/users-table
   [pool-users/user-th-component
    users-membership/member-user-th-component
    users-membership/direct-member-user-th-component
    users-membership/group-member-user-th-component]
   [pool-users/user-td-component
    users-membership/member-user-td-component
    (users-membership/create-direct-member-user-td-component
     direct-member-path)
    (users-membership/create-group-member-user-td-component
     groups-path-fn)]])

(defn page []
  [:article.delegation.my-5
   [delegation/header]

   [:section.mb-5
    [delegation/tabs]
    [users-membership/filter-component]
    [table/toolbar]
    [table]
    [table/toolbar]]

   [debug-component]
   [users/debug-component]
   [delegation/debug-component]])
