(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.groups-roles.front
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
     [leihs.core.core :refer [keyword str presence]]
     [leihs.core.requests.core :as requests]
     [leihs.core.routing.front :as routing]
     [leihs.core.icons :as icons]

     [leihs.admin.front.breadcrumbs :as breadcrumbs]
     [leihs.admin.front.components :as components]
     [leihs.admin.front.state :as state]
     [leihs.admin.paths :as paths :refer [path]]
     [leihs.admin.resources.inventory-pools.inventory-pool.front :as inventory-pool :refer [inventory-pool-id*]]
     [leihs.admin.resources.user.front.shared :as user :refer [user-id* user-data*]]
     [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles :refer [roles-hierarchy allowed-roles-states]]
     [leihs.admin.utils.regex :as regex]

     [accountant.core :as accountant]
     [cljs.core.async :as async]
     [cljs.pprint :refer [pprint]]
     [reagent.core :as reagent]))


(defonce inventory-pool-user-groups-roles-data* (reagent/atom nil))

(def edit-mode?*
  (reaction
    (= (-> @routing/state* :handler-key) :inventory-pool-user-groups-roles)))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:div
      [:h3 "@inventory-pool-user-groups-roles-data*"]
      [:pre (with-out-str (pprint @inventory-pool-user-groups-roles-data*))]]]))

;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fetch-inventory-pool-user-groups-roles-id* (reagent/atom nil))
(defn fetch-inventory-pool-user-groups-roles []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-user-groups-roles (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Inventory-Pool UserGroupsRoles"
                               :handler-key :inventory-pool-user-groups-roles
                               :retry-fn #'fetch-inventory-pool-user-groups-roles}
                              :chan resp-chan)]
    (reset! fetch-inventory-pool-user-groups-roles-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-inventory-pool-user-groups-roles-id*))
            (reset! inventory-pool-user-groups-roles-data*
                    (->> resp :body
                         :groups-roles (map #(assoc % :key (:group_id %))))))))))


(defn clean-and-fetch []
  (reset! inventory-pool-user-groups-roles-data* nil)
  (fetch-inventory-pool-user-groups-roles)
  (user/clean-and-fetch)
  (inventory-pool/clean-and-fetch))


;;; roles component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn header-component []
  [:h1 "Group Roles for "
   [user/user-name-component]
   " in "
   [inventory-pool/name-component]])

(defn groups-roles-component []
  [:div.groups-roles-component
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [debug-component]
   (doall
     (for [group-roles @inventory-pool-user-groups-roles-data*]
       [:div {:key (:group_id group-roles)}
        [:h4.mb-0.mt-3
         "Roles "
         [:span
          [:a.btn.btn-outline-primary.btn-sm
           {:href (path :inventory-pool-group-roles
                        {:inventory-pool-id  @inventory-pool/inventory-pool-id*
                         :group-id (:group_id group-roles)})}
           icons/edit " edit "]
          " via the group "
          [:a {:href (path :group {:group-id (:group_id group-roles)})}
           [:em (:group_name group-roles)]]]]
        [roles/roles-component group-roles]]))])


