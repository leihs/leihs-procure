(ns leihs.admin.front.breadcrumbs
  (:require
    [leihs.admin.front.icons :as icons]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]))

(defn li
  ([k n]
   (li k n {} {}))
  ([handler-key inner route-params query-params]
   (let [active? (= (-> @state/routing-state* :handler-key) handler-key)]
     [:li.breadcrumb-item {:key handler-key :class (if active? "active" "")}
      (if active?
        [:span inner]
        [:a {:href (path handler-key route-params query-params)} inner])])))

(defn api-token-li [user-id api-token-id]
  (li :api-token [:span icons/api-token " API-Token "] {:user-id user-id :api-token-id api-token-id} {}))
(defn api-tokens-li [id]
  (li :api-tokens [:span icons/api-token " API-Tokens "] {:user-id id} {}))
(defn api-token-add-li [id]
  (when (= id (:id @state/user*))
    (li :api-token-add [:span icons/add " Add API-Token "] {:user-id id} {})))
(defn api-token-edit-li [user-id api-token-id]
  (when (= user-id (:id @state/user*))
    (li :api-token-edit [:span icons/edit " Edit API-Token "]
        {:user-id user-id :api-token-id api-token-id} {})))
(defn api-token-delete-li [user-id api-token-id]
  (when (= user-id (:id @state/user*))
    (li :api-token-delete [:span icons/delete " Delete API-Token "]
        {:user-id user-id :api-token-id api-token-id} {})))

(defn admin-li [] (li :admin [:span icons/admin " Admin "]))
(defn auth-li [] (li :auth "Authentication"))
(defn auth-info-li [] (li :auth-info "Info"))
(defn auth-password-sign-in-li [] (li :auth-password-sign-in "Password sign-in"))
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
(defn initial-admin-li [] (li :initial-admin "Initial-Admin"))
(defn leihs-li [] (li :home [:span icons/home " Home "]))
(defn lending-li [] (li :lending "Lending"))
(defn procurement-li [] (li :procurement "Procurement"))
(defn request-li [id] (li :request "Request" {:id id} {}))
(defn requests-li [] (li :requests "Requests"))

(defn user-delete-li [id] (li :user-delete [:span [:i.fas.fa-times] " Delete "] {:user-id id} {}))
(defn user-edit-li [id] (li :user-edit [:span [:i.fas.fa-edit] " Edit "] {:user-id id} {}))
(defn user-li [id] (li :user [:span icons/user " User "] {:user-id id} {}))
(defn user-add-li [] (li :user-new [:span [:i.fas.fa-plus-circle] " Add user "]))
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
