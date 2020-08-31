(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.group.roles.front
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
     [leihs.admin.resources.group.front.shared :as group :refer [group-id* group-data*]]
     [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles :refer [roles-hierarchy allowed-roles-states]]
     [leihs.admin.utils.regex :as regex]

     [accountant.core :as accountant]
     [cljs.core.async :as async]
     [cljs.pprint :refer [pprint]]
     [reagent.core :as reagent]))



(defonce changed?* (reagent/atom false))

(defonce inventory-pool-group-roles-data* (reagent/atom nil))

(def edit-mode?*
  (reaction
    (= (-> @routing/state* :handler-key) :inventory-pool-group-roles)))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@inventory-pool-group-roles-data*"]
      [:pre (with-out-str (pprint @inventory-pool-group-roles-data*))]]]))

;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fetch-inventory-pool-group-roles-id* (reagent/atom nil))
(defn fetch-inventory-pool-group-roles []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-group-roles (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Inventory-Pool UserRoles"
                               :handler-key :inventory-pool-group-roles
                               :retry-fn #'fetch-inventory-pool-group-roles}
                              :chan resp-chan)]
    (reset! fetch-inventory-pool-group-roles-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-inventory-pool-group-roles-id*))
            (reset! inventory-pool-group-roles-data* (:body resp)))))))


(defn clean-and-fetch []
  (reset! changed?* false)
  (reset! inventory-pool-group-roles-data* nil)
  (fetch-inventory-pool-group-roles)
  (group/clean-and-fetch)
  (inventory-pool/clean-and-fetch))


;;; roles component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-group-roles {:inventory-pool-id @inventory-pool-id* :group-id @group-id*})
                               :method :put
                               :json-params  @inventory-pool-group-roles-data*}
                              {:modal true
                               :title "Update Roles"
                               :handler-key :inventory-pool-edit
                               :retry-fn #'put}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (reset! changed?* false))))))

(defn put-submit-component []
  [:div
   [:div.float-right
    [:button.btn.btn-warning
     {:on-click put
      :disabled (not @changed?*)}
     [:i.fas.fa-save]
     " Save "]]
   [:div.clearfix]])

(defn on-change-handler [role]
  (reset! changed?* true)
  (swap! inventory-pool-group-roles-data*
         (fn [data role]
           (let [new-role-state (-> data
                                    (get-in [:roles role])
                                    boolean not)]
             (assoc data :roles
                    (case [role new-role-state]
                      [:customer false] (:none allowed-roles-states)
                      [:customer true] (:customer allowed-roles-states)
                      [:group_manager false] (:customer allowed-roles-states)
                      [:group_manager true] (:group_manager allowed-roles-states)
                      [:lending_manager false] (:group_manager allowed-roles-states)
                      [:lending_manager true] (:lending_manager allowed-roles-states)
                      [:inventory_manager false] (:lending_manager allowed-roles-states)
                      [:inventory_manager true] (:inventory_manager allowed-roles-states)))))
         role))

(defn header-component []
  [:h1 "Roles for the group "
   [:a {:href (path :group {:group-id @group-id*})}
    [group/group-name-component]]
   " in the inventory-pool "
   [:a {:href (path :inventory-pool
                    {:inventory-pool-id @inventory-pool/inventory-pool-id*})}
    [inventory-pool/name-component]]])

(defn roles-component []
  [roles/roles-component
   @inventory-pool-group-roles-data*
   {:edit-mode?  @edit-mode?*
    :on-change-handler on-change-handler}])


(defn page []
  [:div.inventory-pool-group-roles
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [breadcrumbs/nav-component
    [(breadcrumbs/leihs-li)
     (breadcrumbs/admin-li)
     (breadcrumbs/inventory-pools-li)
     (breadcrumbs/inventory-pool-li @inventory-pool/inventory-pool-id*)
     (breadcrumbs/inventory-pool-groups-li @inventory-pool/inventory-pool-id*)
     [breadcrumbs/inventory-pool-group-roles-li @inventory-pool-id* @group-id*]]
    []]
   [header-component]
   [:div.form
    [roles-component]
    [put-submit-component]]
   [debug-component]])
