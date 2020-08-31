(ns leihs.admin.resources.delegations.delegation.users.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.delegations.delegation.front :as delegation :refer [delegation-id*]]
    [leihs.admin.resources.users.front :as users]
    [leihs.admin.shared.membership.users.front :as users-membership :refer []]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [reagent.core :as reagent]))



;;; direct member ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn direct-member-path [user]
  (path :delegation-user {:delegation-id @delegation-id* :user-id (:id user)}))


(def direct-member-user-conf
  {:key :direct
   :th users-membership/direct-member-user-th-component
   :td (users-membership/create-direct-member-user-td-component direct-member-path)})


;;; group member ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn groups-path-fn
  ([user] (groups-path-fn user {} {}))
  ([user more-route-params more-query-params]
   (path :delegation-groups
         (merge
           {:delegation-id @delegation-id* :user-id (:id user)}
           more-route-params)
         (merge
           {:including-user (or (-> user :email presence) (:id user))}
           more-query-params))))

(def group-member-user-conf
  {:key :group
   :th users-membership/group-member-user-th-component
   :td (users-membership/create-group-member-user-td-component
         groups-path-fn)})


;;; colconfig ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def colconfig
  (merge users/default-colconfig
         {:email false
          :customcols [users-membership/member-user-conf
                       direct-member-user-conf
                       group-member-user-conf
                       ]}))


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-mount users/escalate-query-paramas-update
     :did-update users/escalate-query-paramas-update}]
   [users-membership/filter-component]
   [routing/pagination-component]
   [users/users-table-component colconfig]
   [routing/pagination-component]
   [debug-component]
   [users/debug-component]])

(defn index-page []
  [:div.delegation-users
   [routing/hidden-state-component
    {:did-mount (fn [_] (delegation/clean-and-fetch))}]
   (breadcrumbs/nav-component
     [[breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/delegations-li]
      [breadcrumbs/delegation-li @delegation/delegation-id*]
      [breadcrumbs/delegation-users-li @delegation/delegation-id*]][])
   [:div
    [:h1
     [:span " Users in the delegation "]
     [:a
      {:href (path :delegation {:delegation-id @delegation/delegation-id*})}
      [delegation/name-component]]]
    [main-page-component]]])
