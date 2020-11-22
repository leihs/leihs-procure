(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.direct-roles.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
     [leihs.core.core :refer [keyword str presence]]
     [leihs.core.requests.core :as requests]
     [leihs.core.routing.front :as routing]
     [leihs.core.icons :as icons]

     [leihs.admin.common.components :as components]
     [leihs.admin.state :as state]
     [leihs.admin.paths :as paths :refer [path]]
     [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
     [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles :refer [roles-hierarchy allowed-roles-states]]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs :as breadcrumbs]
     [leihs.admin.resources.users.user.core :as user :refer [user-id* user-data*]]
     [leihs.admin.utils.regex :as regex]

     [accountant.core :as accountant]
     [cljs.core.async :as async]
     [cljs.pprint :refer [pprint]]
     [reagent.core :as reagent]))


(defonce inventory-pool-user-direct-roles-data* (reagent/atom nil))

(defonce changed?* (reagent/atom false))

(def edit-mode?*
  (reaction
    (= (-> @routing/state* :handler-key) :inventory-pool-user-direct-roles)))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@inventory-pool-user-direct-roles-data*"]
      [:pre (with-out-str (pprint @inventory-pool-user-direct-roles-data*))]]]))

;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fetch-inventory-pool-user-direct-roles-id* (reagent/atom nil))
(defn fetch-inventory-pool-user-direct-roles []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-user-direct-roles (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Inventory-Pool UserDirectRoles"
                               :handler-key :inventory-pool-user-direct-roles
                               :retry-fn #'fetch-inventory-pool-user-direct-roles}
                              :chan resp-chan)]
    (reset! fetch-inventory-pool-user-direct-roles-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-inventory-pool-user-direct-roles-id*))
            (reset! inventory-pool-user-direct-roles-data* (:body resp)))))))


(defn clean-and-fetch []
  (reset! changed?* false)
  (reset! inventory-pool-user-direct-roles-data* nil)
  (fetch-inventory-pool-user-direct-roles))


;;; roles component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-user-direct-roles
                                          {:inventory-pool-id @inventory-pool/id*
                                           :user-id @user-id*})
                               :method :put
                               :json-params  @inventory-pool-user-direct-roles-data*}
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
  (swap! inventory-pool-user-direct-roles-data*
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
  [:h1 "Direct Roles for "
   [user/name-link-component]
   " in "
   [inventory-pool/name-link-component]])

(defn roles-component []
  [roles/roles-component @inventory-pool-user-direct-roles-data*
   {:edit-mode?  @edit-mode?*
    :on-change-handler on-change-handler}])

(defn page []
  [:div.inventory-pool-user-direct-roles
   [routing/hidden-state-component
    {:did-mount clean-and-fetch}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left* [breadcrumbs/direct-roles-li])[]]
   [header-component]
   [:div.form
    [roles-component]
    [put-submit-component]]
   [debug-component]])
