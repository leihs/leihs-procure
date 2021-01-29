(ns leihs.admin.resources.users.user.inventory-pools
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.user.front :as current-user]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
    [leihs.admin.utils.misc :as front-shared :refer [wait-component]]
    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.common.roles.core :as roles]
    [leihs.admin.resources.users.user.core :as user-core :refer [user-id* user-data*]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))


(defonce data* (reagent/atom nil))

(defn fetch-inventory-pools []
  (defonce fetch-id* (reagent/atom nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user-inventory-pools
                                          (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Inventory Pool Roles"
                               :retry-fn #'fetch-inventory-pools}
                              :chan resp-chan)]
    (reset! fetch-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-id*))
            (reset! data*
                    (-> resp :body :user-inventory-pools)))))))

(defn clean-and-fetch-inventory-pools [& args]
  (reset! data* nil)
  (fetch-inventory-pools))

(defn debug-component []
  [:div
   (when (:debug @state/global-state*)
     [:div.inventory-pools-debug
      [:hr]
      [:div.inventory-pools-data
       [:h3 "@data*"]
       [:pre (with-out-str (pprint @data*))]]])])

(defn user-in-pool-td-component [row]
  (let [inventory-pool-id (:inventory_pool_id row)
        has-access? (pool-auth/current-user-is-some-manager-of-pool? inventory-pool-id)
        pool-path (path :inventory-pool
                        {:inventory-pool-id (:inventory_pool_id row)})
        user-in-pool-path (path :inventory-pool-user
                                {:inventory-pool-id (:inventory_pool_id row)
                                 :user-id @user-id*})
        user-in-pool-inner [:em (user-core/fullname-or-some-uid @user-data*)]
        pool-inner [:em (:inventory_pool_name row )]]
    [:td
     [:span
      (if has-access?
        [:a {:href user-in-pool-path} user-in-pool-inner]
        user-in-pool-inner)
      " in "
      (if has-access?
        [:a {:href pool-path} pool-inner]
        pool-inner )]]))

(defn roles-td-component [row]
  [:td
   (->> row :role roles/expand-to-hierarchy
        (map str)
        (clojure.string/join ", "))])

(defn contracts-td-component [row]
  [:td
   (:open_contracts_count row)
   " / "
   (:contracts_count row)])

(defn reservations-td-component [row]
  [:td
   (:submitted_reservations_count row)
   " ; "
   (:approved_reservations_count row)
   " / "
   (:reservations_count row)])

(defn table-component []
  [:div.user-inventory-pools
   [routing/hidden-state-component
    {:did-mount clean-and-fetch-inventory-pools
     :did-change clean-and-fetch-inventory-pools}]
   (if (and @data* @user-data*)
     [:table.user-inventory-pools.table.table-sm.table-striped
      [:thead
       [:tr
        [:th "User in pool"]
        [:th "Roles"]
        [:th "Submitted ; approved / total reservations"]
        [:th "Open / total contracts"]]]
      [:tbody
       (for [row (->>  @data* (sort-by :inventory_pool_name))]
         [:tr.pool {:key (:inventory_pool_id row)}
          [user-in-pool-td-component row]
          [roles-td-component row]
          [reservations-td-component row]
          [contracts-td-component row]])]]
     [wait-component])
   [debug-component]])

