(ns leihs.admin.resources.delegations.delegation.groups.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.icons :as icons]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.delegations.delegation.front :as delegation :refer [delegation-id* name-component]]
    [leihs.admin.resources.groups.front :as groups]
    [leihs.admin.shared.membership.groups.front :as groups-membership]
    [leihs.admin.utils.regex :as regex]

    [clojure.contrib.inflect :refer [pluralize-noun]]
    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))

(defn member-path
  ([group]
   (member-path group {}))
  ([group query-params]
   (path :delegation-group
         {:delegation-id @delegation-id*
          :group-id (:id group)} query-params)))

;### filter ###################################################################

(defn filter-component []
  [:div.card.bg-light
   [:div.card-body
   [:div.form-row
    [groups/form-term-filter]
    [groups/form-including-user-filter]
    [groups-membership/form-membership-filter]
    [routing/form-per-page-component]
    [routing/form-reset-component]]]])


;### header ###################################################################

(defn header-component []
  [:h1
   [:span "Groups of Entitlement-Group "]
   [:a
    {:href (path :delegation {:delegation-id @delegation-id*})}
    [:span [name-component]]]])


;### main #####################################################################

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div ]))

(defn main-page-component []
  [:div
   [routing/hidden-state-component
    {:did-mount groups/escalate-query-paramas-update
     :did-update groups/escalate-query-paramas-update}]
   [filter-component]
   [routing/pagination-component]
   [groups/groups-table-component
    [groups-membership/member-th-component]
    [(partial groups-membership/member-td-component member-path)]]
   [routing/pagination-component]
   [debug-component]
   [groups/debug-component]])

(defn page []
  [:div.inventory-pool-groups
   [routing/hidden-state-component
    {:did-mount (fn [_] (delegation/clean-and-fetch))}]
   [breadcrumbs/nav-component
    [[breadcrumbs/leihs-li]
     [breadcrumbs/admin-li]
     [breadcrumbs/delegations-li]
     [breadcrumbs/delegation-li @delegation-id*]
     [breadcrumbs/delegation-groups-li @delegation-id*]][]]

   [:div
    [header-component]
    [main-page-component]]])
