(ns leihs.admin.front.breadcrumbs (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.breadcrumbs :as core-breadcrumbs]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]

    [leihs.admin.paths :as paths :refer [path]]))

(def li core-breadcrumbs/li)

(defn admin-li [] [li :admin [:span icons/admin " Admin "]])
(defn auth-li [] [li :auth "Authentication"])
(defn auth-info-li [] [li :auth-info "Info"])
(defn auth-password-sign-in-li [] [li :password-authentication "Password sign-in"])
(defn borrow-li [] [li :borrow "Borrow"])
(defn debug-li [] [li :debug "Debug"])

(defn delegation-delete-li [id] [li :delegation-delete [:span [:i.fas.fa-times] " Delete "] {:delegation-id id} {}])
(defn delegation-edit-li [id] [li :delegation-edit [:span [:i.fas.fa-edit] " Edit "] {:delegation-id id} {}])
(defn delegation-li [id] [li :delegation [:span icons/delegation " Delegation "] {:delegation-id id} {}])
(defn delegation-add-li [] [li :delegation-add [:span [:i.fas.fa-plus-circle] " Add delegation "]])
(defn delegation-users-li [delegation-id]
  [li :delegation-users
      [:span icons/users " Users "]
      {:delegation-id delegation-id} {}])
(defn delegations-li [] [li :delegations [:span icons/delegations " Delegations "]])


(defn group-add-li [] [li :group-add [:span [:i.fas.fa-plus-circle] " Add group "]])
(defn group-delete-li [id] [li :group-delete [:span [:i.fas.fa-times] " Delete "] {:group-id id} {}])
(defn group-edit-li [id] [li :group-edit [:span [:i.fas.fa-edit] " Edit "] {:group-id id} {}])
(defn group-inventory-pools-rooles-li [id] [li :group-inventory-pools-roles [:span " Inventory Pool Roles "] {:group-id id} {}])
(defn group-li [id] [li :group [:span icons/group " Group "] {:group-id id} {}])
(defn group-users-li [group-id] [li :group-users [:span icons/users " Users "] {:group-id group-id} {}])
(defn groups-li [] [li :groups [:span icons/groups " Groups "] {} {}])

(defn inventory-pool-add-li [] [li :inventory-pool-add [:span [:i.fas.fa-plus-circle] " Add Inventory-Pool "]])
(defn inventory-pool-delete-li [id] [li :inventory-pool-delete [:span [:i.fas.fa-times] " Delete "] {:inventory-pool-id id} {}])
(defn inventory-pool-edit-li [id] [li :inventory-pool-edit [:span [:i.fas.fa-edit] " Edit "] {:inventory-pool-id id} {}])
(defn inventory-pool-group-li [inventory-pool-id group-id] [li :inventory-pool-group [:span icons/group " Group "] {:inventory-pool-id inventory-pool-id :group-id group-id} {}])
(defn inventory-pool-group-roles-li [inventory-pool-id group-id] [li :inventory-pool-group-roles [:span icons/edit " Manage Group Roles "] {:inventory-pool-id inventory-pool-id :group-id group-id} {}])
(defn inventory-pool-groups-li [inventory-pool-id] [li :inventory-pool-groups [:span icons/groups " Groups "] {:inventory-pool-id inventory-pool-id} {}])
(defn inventory-pool-li [id] [li :inventory-pool [:span icons/inventory-pool " Inventory-Pool "] {:inventory-pool-id id} {}])
(defn inventory-pool-user-li [inventory-pool-id user-id] [li :inventory-pool-user [:span icons/user " User "] {:inventory-pool-id inventory-pool-id :user-id user-id} {}])
(defn inventory-pool-user-roles-li [inventory-pool-id user-id] [li :inventory-pool-user-roles [:span icons/edit " Manage Roles "] {:inventory-pool-id inventory-pool-id :user-id user-id} {}])
(defn inventory-pool-user-direct-roles-li [inventory-pool-id user-id] [li :inventory-pool-user-direct-roles [:span icons/edit " Direct Roles "] {:inventory-pool-id inventory-pool-id :user-id user-id} {}])
(defn inventory-pool-user-suspension-li [inventory-pool-id user-id] [li :inventory-pool-user-suspension [:span icons/edit " Suspension"] {:inventory-pool-id inventory-pool-id :user-id user-id} {}])
(defn inventory-pool-users-li [inventory-pool-id] [li :inventory-pool-users [:span icons/users " Users "] {:inventory-pool-id inventory-pool-id} {}])
(defn inventory-pools-li [] [li :inventory-pools [:span icons/inventory-pools " Inventory-Pools "]])

(defn email-li [address] [:li.breadcrumb-item {:key (str "mailto:" address )} [:a {:href (str "mailto:" address )} [:i.fas.fa-envelope] " Email "]])
(defn leihs-li [] [li :home [:span icons/home " Home "]])
(defn lending-li [] [li :lending "Lending"])
(defn procurement-li [] [li :procurement "Procurement"])
(defn request-li [id] [li :request "Request" {:id id} {}])
(defn requests-li [] [li :requests "Requests"])

(defn user-add-li [] [li :user-new [:span [:i.fas.fa-plus-circle] " Add user "]])
(defn user-delete-li [id] [li :user-delete [:span [:i.fas.fa-times] " Delete "] {:user-id id} {}])
(defn user-edit-li [id] [li :user-edit [:span [:i.fas.fa-edit] " Edit "] {:user-id id} {}])
(defn user-inventory-pools-rooles-li [id] [li :user-inventory-pools-roles [:span " Inventory Pool Roles "] {:user-id id} {}])
(defn user-li [id] [li :user [:span icons/user-in-admin " User "] {:user-id id} {}])
(defn user-my-li [id] [li :my-user [:span icons/user " User-Home "] {:user-id id} {}])
(defn users-li [] [li :users [:span icons/users " Users "] {} {}])

(defn nav-component [lefts rights]
  [:div.row.nav-component.mt-3
   [:nav.col-lg {:key :nav-left :aria-label :breadcrumb :role :navigation}
    (when (seq lefts)
      [:ol.breadcrumb
       (doall (map-indexed (fn [idx item] [:<> {:key idx} item]) lefts))])]
   [:nav.col-lg {:key :nav-right :role :navigation}
    (when (seq rights)
      [:ol.breadcrumb.leihs-nav-right
       (doall (map-indexed (fn [idx item] [:<> {:key idx} item]) rights))])]])
