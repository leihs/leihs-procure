(ns leihs.admin.resources.user.front.inventory-pools-roles
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.shared :refer [humanize-datetime-component gravatar-url]]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles]
    [leihs.admin.resources.user.front.shared :as user.shared :refer [clean-and-fetch user-id* user-data* edit-mode?*]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))


(defonce inventory-pools-roles-data* (reagent/atom nil))

(defonce fetch-inventory-pools-roles-id* (reagent/atom nil))

(defn fetch-inventory-pools-roles []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user-inventory-pools-roles
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
            (reset! inventory-pools-roles-data*
                    (-> resp :body :inventory_pools_roles)))))))

(defn clean-and-fetch-inventory-pools-roles [& args]
  (clean-and-fetch)
  (reset! inventory-pools-roles-data* nil)
  (fetch-inventory-pools-roles))

(defn inventory-pools-roles-debug-component []
  [:div
   (when (:debug @state/global-state*)
     [:div.inventory-pools-roles-debug
      [:hr]
      [:div.inventory-pools-roles-data
       [:h3 "@inventory-pools-roles-data*"]
       [:pre (with-out-str (pprint @inventory-pools-roles-data*))]]])
   [user.shared/debug-component]])

(defn roles-table-component []
  [:div
   [:table.roles.table
    [:thead
     [:tr [:th "Pool"] [:th "Roles"]]]
    [:tbody
     (for [row (->>  @inventory-pools-roles-data*
                    (sort-by :inventory_pool_name))]
       [:tr.pool {:key (:inventory_pool_id row)}
        [:td
         [:a {:href (path :inventory-pool
                          {:inventory-pool-id (:inventory_pool_id row)})}
          [:em (:inventory_pool_name row )]] ""]
        [:td
         [:a {:href (path :inventory-pool-user
                          {:inventory-pool-id (:inventory_pool_id row)
                           :user-id (:user_id row)})}
          (->> row :role roles/expand-role-to-hierarchy
               (map str)
               (clojure.string/join ", "))]]])]]])


(defn page []
  [:div.user-inventory-pools-roles
   [routing/hidden-state-component
    {:did-mount clean-and-fetch-inventory-pools-roles
     :did-change clean-and-fetch-inventory-pools-roles}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/users-li)
      (breadcrumbs/user-li @user-id*)
      (breadcrumbs/user-inventory-pools-rooles-li @user-id*)]
     [])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " User "]
      [user.shared/user-name-component]
      " Inventory Pool Roles"]
     [user.shared/user-id-component]]]
   [roles-table-component]
   [inventory-pools-roles-debug-component]])
