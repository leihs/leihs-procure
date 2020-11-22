(ns leihs.admin.resources.groups.group.inventory-pools
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.common.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles]
    [leihs.admin.resources.groups.group.core :as group.shared :refer [group-id* edit-mode?*]]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))


(defonce data* (reagent/atom nil))

(defn prepare-inventory-pools-data [data]
  (->> data
       (reduce (fn [roles role]
                 (-> roles
                     (assoc-in [(:inventory_pool_id role) :name] (:inventory_pool_name role))
                     (assoc-in [(:inventory_pool_id role) :id] (:inventory_pool_id role))
                     (assoc-in [(:inventory_pool_id role) :key] (:inventory_pool_id role))
                    ; (assoc-in [(:inventory_pool_id role) :role (:role role)] role)
                     ))
               {})
       (map (fn [[_ v]] v))
       (sort-by :name)
       (into [])))

(defonce fetch-inventory-pools-roles-id* (reagent/atom nil))

(defn fetch-inventory-pools-roles []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :group-inventory-pools-roles
                                          (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Inventory Pool Roles"
                               :retry-fn #'fetch-inventory-pools-roles}
                              :chan resp-chan)]
    (reset! fetch-inventory-pools-roles-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-inventory-pools-roles-id*))
            (reset! data*
                    (-> resp :body :inventory_pools_roles ;prepare-inventory-pools-data
                        )))))))

(defn clean-and-fetch [& args]
  (reset! data* nil)
  (fetch-inventory-pools-roles))

(defn inventory-pools-roles-debug-component []
  [:div
   (when (:debug @state/global-state*)
     [:div.inventory-pools-roles-debug
      [:hr]
      [:div.inventory-pools-roles-data
       [:h3 "@data*"]
       [:pre (with-out-str (pprint @data*))]]])
   [group.shared/debug-component]])

(defn table-component []
  [:div
   [routing/hidden-state-component
    {:did-change clean-and-fetch}]
   [:table.roles.table
    [:thead
     [:tr [:th "Pool"] [:th "Roles"]]]
    [:tbody
     (for [row (->>  @data*
                    (sort-by :inventory_pool_name))]
       [:tr.pool {:key (:inventory_pool_id row)}
        [:td
         [:a {:href (path :inventory-pool
                          {:inventory-pool-id (:inventory_pool_id row)})}
          [:em (:inventory_pool_name row )]] ""]
        [:td
         [:a {:href (path :inventory-pool-group-roles
                          {:inventory-pool-id (:inventory_pool_id row)
                           :group-id (:group_id row)})}
          (->> row :role roles/expand-role-to-hierarchy
               (map str)
               (clojure.string/join ", "))]]])]]])
