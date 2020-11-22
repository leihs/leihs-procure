(ns leihs.admin.resources.inventory-pools.inventory-pool.users.user.roles.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
     [leihs.core.core :refer [keyword str presence]]
     [leihs.core.requests.core :as requests]
     [leihs.core.routing.front :as routing]
     [leihs.core.icons :as icons]

     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs :as breadcrumbs]
     [leihs.admin.common.components :as components]
     [leihs.admin.state :as state]
     [leihs.admin.paths :as paths :refer [path]]
     [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
     [leihs.admin.resources.users.user.core :as user :refer [user-id* user-data*]]
     [leihs.admin.resources.inventory-pools.inventory-pool.roles :as roles :refer [roles-hierarchy allowed-roles-states]]
     [leihs.admin.utils.regex :as regex]

     [accountant.core :as accountant]
     [cljs.core.async :as async]
     [cljs.pprint :refer [pprint]]
     [reagent.core :as reagent]))


(defonce inventory-pool-user-roles-data* (reagent/atom nil))

(def edit-mode?*
  (reaction
    (= (-> @routing/state* :handler-key) :inventory-pool-user-roles)))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@inventory-pool-user-roles-data*"]
      [:pre (with-out-str (pprint @inventory-pool-user-roles-data*))]]]))

;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fetch-inventory-pool-user-roles-id* (reagent/atom nil))
(defn fetch-inventory-pool-user-roles []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-user-roles (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Inventory-Pool UserRoles"
                               :handler-key :inventory-pool-user-roles
                               :retry-fn #'fetch-inventory-pool-user-roles}
                              :chan resp-chan)]
    (reset! fetch-inventory-pool-user-roles-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-inventory-pool-user-roles-id*))
            (reset! inventory-pool-user-roles-data* (:body resp)))))))


(defn clean-and-fetch []
  (reset! inventory-pool-user-roles-data* nil)
  (fetch-inventory-pool-user-roles)
  (user/clean-and-fetch)
  (inventory-pool/clean-and-fetch))


;;; roles component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn put [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-user-roles {:inventory-pool-id @inventory-pool/id* :user-id @user-id*})
                               :method :put
                               :json-params  @inventory-pool-user-roles-data*}
                              {:modal true
                               :title "Update Roles"
                               :handler-key :inventory-pool-edit
                               :retry-fn #'put}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :inventory-pool-user {:inventory-pool-id @inventory-pool/id* :user-id @user-id*})))))))

(defn put-submit-component []
  [:div
   [:div.float-right
    [:button.btn.btn-warning
     {:on-click put}
     [:i.fas.fa-save]
     " Save "]]
   [:div.clearfix]])

(defn on-change-handler [role]
  (swap! inventory-pool-user-roles-data*
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
  [:h1 "Roles for "
   [user/name-link-component]
   " in "
   [inventory-pool/name-link-component]])

(defn remarks-component []
  [:div
   [:p "These roles represent an " [:strong  "aggregate"] " state derived from "
    [:strong " direct roles"] ", " " e.i. inventory-pool to user roles, and roles given "
    [:strong "through groups"] "."
    " The resource behind these roles is for " [:strong  " compatibility reasons writable"]
    " as long as the aggregate is backed soly by direct roles. "
    " The ability to write to this resource is " [:strong  "deprecated" ]
    " and will be removed in future versions of leihs. "]])

(defn roles-component []
  [roles/roles-component @inventory-pool-user-roles-data*
   {:edit-mode? @edit-mode?*
    :on-change-handler on-change-handler}])

(defn page []
  [:div.inventory-pool-user-roles
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left*
          [breadcrumbs/roles-li]) []]
   [header-component]
   [remarks-component]
   [:div.form
    [roles-component]
    [put-submit-component]]
   [debug-component]])
