(ns leihs.admin.front.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.breadcrumbs :as core-breadcrumbs]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]

    [leihs.admin.paths :as paths :refer [path]]))

(def li core-breadcrumbs/li)

(defn admin-li [] (li :admin [:span icons/admin " Admin "]))
(defn auth-li [] (li :auth "Authentication"))
(defn auth-info-li [] (li :auth-info "Info"))
(defn auth-password-sign-in-li [] (li :password-authentication "Password sign-in"))
(defn borrow-li [] (li :borrow "Borrow"))
(defn debug-li [] (li :debug "Debug"))

(defn delegation-delete-li [id] (li :delegation-delete [:span [:i.fas.fa-times] " Delete "] {:delegation-id id} {}))
(defn delegation-edit-li [id] (li :delegation-edit [:span [:i.fas.fa-edit] " Edit "] {:delegation-id id} {}))
(defn delegation-li [id] (li :delegation [:span icons/delegation " Delegation "] {:delegation-id id} {}))
(defn delegation-add-li [] (li :delegation-add [:span [:i.fas.fa-plus-circle] " Add delegation "]))
(defn delegation-users-li [delegation-id]
  (li :delegation-users
      [:span icons/users " Users "]
      {:delegation-id delegation-id} {}))
(defn delegations-li [] (li :delegations [:span icons/delegations " Delegations "]))


(defn group-delete-li [id] (li :group-delete [:span [:i.fas.fa-times] " Delete "] {:group-id id} {}))
(defn group-edit-li [id] (li :group-edit [:span [:i.fas.fa-edit] " Edit "] {:group-id id} {}))
(defn group-li [id] (li :group [:span icons/group " Group "] {:group-id id} {}))
(defn group-add-li [] (li :group-add [:span [:i.fas.fa-plus-circle] " Add group "]))
(defn groups-li [] (li :groups [:span icons/groups " Groups "] {} {}))
(defn group-users-li [group-id]
  (li :group-users
      [:span icons/users " Users "]
      {:group-id group-id} {}))


(defn email-li [address] [:li.breadcrumb-item {:key (str "mailto:" address )} [:a {:href (str "mailto:" address )} [:i.fas.fa-envelope] " Email "]])
(defn leihs-li [] (li :home [:span icons/home " Home "]))
(defn lending-li [] (li :lending "Lending"))
(defn procurement-li [] (li :procurement "Procurement"))
(defn request-li [id] (li :request "Request" {:id id} {}))
(defn requests-li [] (li :requests "Requests"))

(defn user-add-li [] (li :user-new [:span [:i.fas.fa-plus-circle] " Add user "]))
(defn user-delete-li [id] (li :user-delete [:span [:i.fas.fa-times] " Delete "] {:user-id id} {}))
(defn user-edit-li [id] (li :user-edit [:span [:i.fas.fa-edit] " Edit "] {:user-id id} {}))
(defn user-inventory-pools-rooles-li [id] (li :user-inventory-pools-roles [:span " Inventory-Pools-Roles "] {:user-id id} {}))
(defn user-li [id] (li :user [:span icons/user " User "] {:user-id id} {}))
(defn user-password-li [id] (li :user-password [:span icons/password " Password "] {:user-id id} {}))
(defn users-li [] (li :users [:span icons/users " Users "] {} {}))

(defn nav-component [left right]
  [:div.row.nav-component.mt-3
   [:nav.col-lg {:aria-label :breadcrumb :role :navigation}
    (when (seq left)
      [:ol.breadcrumb
       (for [li left] li) ])]
   [:nav.col-lg {:role :navigation}
    (when (seq right)
      [:ol.breadcrumb.leihs-nav-right
       (for [li right] li)])]])
