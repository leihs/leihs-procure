(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.create
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.admin.common.icons :as icons]

    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.users.user.create :as create]
    [leihs.admin.resources.users.user.edit-core :as edit-core :refer [data*]]
    [leihs.admin.resources.users.user.edit-main :as edit-main]
    [leihs.admin.state :as state]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]

    [taoensso.timbre :as logging]
    ))


(defn post [& args]
  (go (when-let [id (some->
                      {:chan (async/chan)
                       :url (path :users)
                       :method :post
                       :json-params  (-> @data*
                                         (update-in
                                           [:extended_info]
                                           (fn [s] (.parse js/JSON s))))}
                      http-client/request :chan <!
                      http-client/filter-success! :body :id)]

        (accountant/navigate!
          (path :inventory-pool-user
                {:inventory-pool-id @inventory-pool/id*
                 :user-id id})))))

(defn page []
  [:div.user-create
   [routing/hidden-state-component
    {:did-mount create/clean}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/create-li])[]]
   [:h1 "Create User in the Inventory-Pool " [inventory-pool/name-link-component]]
   [create/edit-form-component (fn [e]
                                 (.preventDefault e)
                                 (post))]
   [edit-core/debug-component]])
