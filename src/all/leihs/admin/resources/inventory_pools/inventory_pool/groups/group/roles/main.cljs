(ns leihs.admin.resources.inventory-pools.inventory-pool.groups.group.roles.main
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
     [leihs.admin.common.roles.components :refer [roles-component fetch-roles< put-roles<]]
     [leihs.admin.common.roles.core :as roles]
     [leihs.admin.paths :as paths :refer [path]]
     [leihs.admin.resources.groups.group.core :as group :refer [group-id*]]
     [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
     [leihs.admin.resources.inventory-pools.inventory-pool.groups.group.breadcrumbs :as breadcrumbs]
     [leihs.admin.state :as state]
     [leihs.admin.utils.regex :as regex]

     [accountant.core :as accountant]
     [cljs.core.async :as async]
     [cljs.pprint :refer [pprint]]
     [reagent.core :as reagent]))



(defonce changed?* (reagent/atom false))

(defonce data* (reagent/atom nil))

(def edit-mode?*
  (reaction
    (= (-> @routing/state* :handler-key) :inventory-pool-group-roles)))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fetch-inventory-pool-group-roles-id* (reagent/atom nil))
(defn fetch-inventory-pool-group-roles []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-group-roles (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Inventory-Pool Group Roles"
                               :handler-key :inventory-pool-group-roles
                               :retry-fn #'fetch-inventory-pool-group-roles}
                              :chan resp-chan)]
    (reset! fetch-inventory-pool-group-roles-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-inventory-pool-group-roles-id*))
            (reset! data* (:body resp)))))))


(defn clean-and-fetch []
  (reset! changed?* false)
  (reset! data* nil)
  (fetch-inventory-pool-group-roles)
  (group/clean-and-fetch)
  (inventory-pool/clean-and-fetch))

(defn header-component []
  [:h1 "Roles for the group "
   [group/group-name-component]
   " in the inventory-pool "
   [inventory-pool/name-link-component]])

(defn page []
  [:div.inventory-pool-group-roles
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [breadcrumbs/nav-component
    (conj @breadcrumbs/left*
          [breadcrumbs/group-roles-li])[]]
   [header-component]
   [:div.form
    [roles-component @data*
     :update-handler #(go (reset! data*
                                  (<! (put-roles<
                                        (path :inventory-pool-group-roles
                                              (:route-params @routing/state*))
                                        %))))]]
   [debug-component]])
