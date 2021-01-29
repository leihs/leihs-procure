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

     [leihs.admin.common.components :as components]
     [leihs.admin.common.roles.components :as roles-ui :refer [fetch-roles< put-roles<]]
     [leihs.admin.common.roles.core :as roles]
     [leihs.admin.paths :as paths :refer [path]]
     [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
     [leihs.admin.resources.inventory-pools.inventory-pool.users.user.breadcrumbs :as breadcrumbs]
     [leihs.admin.resources.users.user.core :as user :refer [user-id* user-data*]]
     [leihs.admin.state :as state]
     [leihs.admin.utils.regex :as regex]

     [accountant.core :as accountant]
     [cljs.core.async :as async]
     [cljs.pprint :refer [pprint]]
     [reagent.core :as reagent]))


(defonce roles-data* (reagent/atom nil))

(def edit-mode?*
  (reaction
    (= (-> @routing/state* :handler-key) :inventory-pool-user-roles)))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@roles-data*"]
      [:pre (with-out-str (pprint @roles-data*))]]]))

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
            (reset! roles-data* (:body resp)))))))


(defn clean-and-fetch []
  (reset! roles-data* nil)
  (fetch-inventory-pool-user-roles)
  (user/clean-and-fetch)
  (inventory-pool/clean-and-fetch))


;;; roles component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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
   [roles-ui/roles-component @roles-data*
    :update-handler #(go (reset! roles-data*
                                 (<! (put-roles<
                                       (path :inventory-pool-user-roles
                                             (:route-params @routing/state*))
                                       %))))]
   [debug-component]])
