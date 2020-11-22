(ns leihs.admin.common.breadcrumbs
  (:refer-clojure :exclude [str keyword])
  (:require
    [leihs.core.breadcrumbs :as core-breadcrumbs]
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]

    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.core.auth.core :as auth]
    [leihs.admin.paths :as paths :refer [path]]))

(def li core-breadcrumbs/li)

;;;        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn admin-li [] [li :admin [:span icons/admin " Admin "] {} {}
                   :authorizers [auth/all-granted]])
(defn auth-li [] [li :auth "Authentication"])
(defn auth-info-li [] [li :auth-info "Info"])
(defn auth-password-sign-in-li [] [li :password-authentication "Password sign-in"])
(defn borrow-li [] [li :borrow "Borrow"])
(defn debug-li [] [li :debug "Debug"])


(defn groups-li []
  [li :groups [:span icons/groups " Groups "] {} {}
   :authorizers [auth/admin-scopes?
                 pool-auth/some-lending-manager?]])


;;; Other ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn email-li [address]
  [:li.breadcrumb-item {:key (str "mailto:" address )}
   [:a {:href (str "mailto:" address )} [:i.fas.fa-envelope] " Email "]])

(defn leihs-li [] [li :home [:span icons/home " Home "] {} {}
                   :authorizers [auth/all-granted]])
(defn lending-li [] [li :lending "Lending"])
(defn procurement-li [] [li :procurement "Procurement"])
(defn request-li [id] [li :request "Request" {:id id} {}])
(defn requests-li [] [li :requests "Requests"])


;;; User(s) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user-create-li []
  [li :user-create [:span [:i.fas.fa-plus-circle] " Create user "] {} {}
   :button true
   :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]])

(defn user-li [id]
  [li :user [:span icons/user " User "] {:user-id id} {}
   :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]])

(defn user-my-li [id]
  [li :my-user [:span icons/user
                " User-Home in leihs/my
                " [:i.fas.fa-external-link-alt]]
   {:user-id id} {}
   :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]])

(defn users-li []
  [li :users [:span icons/users " Users "] {} {}
   :authorizers [auth/admin-scopes? pool-auth/some-lending-manager?]])

(defn users-choose-li []
  [li :users-choose [:span  " Choose user "] {} {}
   :authorizers [auth/admin-scopes?]])

;;; nav-component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn nav-component [lefts rights]
  [:div.row.nav-component.mt-3.breadcrumbs-bar
   [:nav.col-lg {:key :nav-left :aria-label :breadcrumb :role :navigation}
    (when (seq lefts)
      [:ol.breadcrumb
       (doall (map-indexed (fn [idx item] [:<> {:key idx} item]) lefts))])]
   [:nav.col-lg.breadcrumbs-right
    {:key :nav-right :role :navigation}
    (when (seq rights)
      [:ol.breadcrumb.leihs-nav-right
       (doall (map-indexed (fn [idx item] [:<> {:key idx} item]) rights))])]])
