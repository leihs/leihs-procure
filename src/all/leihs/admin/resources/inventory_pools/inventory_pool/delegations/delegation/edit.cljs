(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.edit
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]

    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as delegation]
    [leihs.admin.resources.inventory-pools.inventory-pool.delegations.main :as delegations]
    [leihs.admin.state :as state]
    [leihs.admin.utils.regex :as regex]

    [clojure.set :refer [rename-keys]]
    [cljs.core.async :as async :refer [timeout]]
    [accountant.core :as accountant]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    [taoensso.timbre :as logging]))

(defonce data* (reagent/atom {}))

(defn set-data-by-query-params [& _]
  (reset! data*
          (merge {:pool_protected true}
                 (-> @routing/state*
                     :query-params
                     (select-keys [:name :responsible_user_id :user-uid :pool_protected])
                     (rename-keys {:user-uid :responsible_user_id})))))

;;; form ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn responsible-user-choose-component []
  [:div.input-group-append
   [:a.btn.btn-info
    {:tab-index form-components/TAB-INDEX
     :href (path :users-choose {}
                 {:return-to (path (:handler-key @routing/state*)
                                   (:route-params @routing/state*)
                                   @data*)})}
    [:i.fas.fa-rotate-90.fa-hand-pointer.px-2]
    " Choose responsible user "]])

;;; form ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn form-component [action submit-component]
  [:form.form.mt-2
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (action))}
   [form-components/input-component data* [:name]
    :label "Name"]
   [form-components/input-component data* [:responsible_user_id]
    :label "Responsible user"
    :append responsible-user-choose-component]
   [:div
    [form-components/checkbox-component data* [:pool_protected]
     :label "Protected"
     :hint [:span
            "An " [:strong " unprotected " ]
            " delegation can be " [:strong "added"] " to any other pool and then be used and "
            [:strong  " modified "] " from those pool in every way."
            " You can unprotect an delegation temporarily to share it with a limited number of pools."]]]
   [submit-component]])

;;; edit / patch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [& more]
  (let [route (path :inventory-pool-delegation
                    {:inventory-pool-id @inventory-pool/id*
                     :delegation-id @delegation/id*})]
    (go (when (some->
                {:chan (async/chan)
                 :url route
                 :method :patch
                 :json-params  @data*}
                http-client/request :chan <!
                http-client/filter-success!)
          (accountant/navigate! route)))))

(defn edit-delegation-form-component []
  [form-component patch form-components/save-submit-component])

(defn edit-page []
  [:div.edit-delegation
   [routing/hidden-state-component
    {:did-mount set-data-by-query-params}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left*
          [breadcrumbs/edit-li])[]]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Delegation "]
      [delegation/name-link-component]]]]
   [edit-delegation-form-component]
   [delegation/debug-component]])


;;; new / post ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [& _]
  (go (when-let [id (some->
                      {:chan (async/chan)
                       :url (path :inventory-pool-delegations
                                  {:inventory-pool-id @inventory-pool/id*})
                       :method :post
                       :json-params @data*}
                      http-client/request :chan <!
                      http-client/filter-success! :body :id)]
        (accountant/navigate!
          (path :inventory-pool-delegation {:inventory-pool-id @inventory-pool/id*
                                            :delegation-id id})))))

(defn new-delegation-form-component []
  [form-component create form-components/create-submit-component])

(defn new-page []
  [:div.new-delegation
   [routing/hidden-state-component
    {:did-mount set-data-by-query-params}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left*
          [breadcrumbs/create-li])[]]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Create a new Delegation "]
      [:span " in the Inventory-Pool "]
      [inventory-pool/name-link-component]]]]
   [new-delegation-form-component]])

