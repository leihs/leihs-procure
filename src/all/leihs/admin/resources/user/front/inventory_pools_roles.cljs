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


(def inventory-pools-roles-data* (reagent/atom nil))

(defn prepare-inventory-pools-data [data]
  (->> data
       (reduce (fn [roles role]
                 (-> roles
                     (assoc-in [(:inventory_pool_id role) :name] (:inventory_pool_name role))
                     (assoc-in [(:inventory_pool_id role) :id] (:inventory_pool_id role))
                     (assoc-in [(:inventory_pool_id role) :key] (:inventory_pool_id role))
                     (assoc-in [(:inventory_pool_id role) :roles (:role role)] role)))
               {})
       (map (fn [[_ v]] v))
       (sort-by :name)
       (into [])))

(defonce fetch-inventory-pools-roles-id* (reagent/atom nil))

(defn fetch-inventory-pools-roles []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :user-inventory-pools-roles
                                          (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Inventory-Pools-Roles"
                               :retry-fn #'fetch-inventory-pools-roles}
                              :chan resp-chan)]
    (reset! fetch-inventory-pools-roles-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-inventory-pools-roles-id*))
            (reset! inventory-pools-roles-data*
                    (-> resp :body :inventory_pools_roles prepare-inventory-pools-data)))))))

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
      " Inventory-Pools-Roles"]
     [user.shared/user-id-component]]]
   [:div.roles
    [:h1 "Active Roles"]
    (for [pool @inventory-pools-roles-data*]
      [:div.pool {:key (:id pool)}
       [:h2 "Pool \"" (:name pool) "\""]
       [:ul
        (for [[_ role] (:roles pool)]
          [:li {:key (:id role)} (:role role)])]])]
   [inventory-pools-roles-debug-component]])


