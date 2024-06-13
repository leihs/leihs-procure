(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components :as components :refer [link]]
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.state :as state]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front]
   [leihs.core.user.shared :refer [short-id]]
   [react-bootstrap :as react-bootstrap]
   [reagent.core :as reagent :refer [reaction]]))

(defonce id* (reaction (or (-> @routing/state* :route-params :delegation-id)
                           ":delegation-id")))

(defonce data* (reagent/atom nil))
(defonce delegation* (reaction (get @data* @id*)))

(defn fetch-delegation []
  (go (swap! data* assoc @id*
             (some-> {:url (path :inventory-pool-delegation
                                 (-> @routing/state* :route-params))
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success! :body))))

;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch [_]
  (fetch-delegation))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.delegation-debug
     [:hr]
     [:div.delegation-id
      [:h3 "@id*"]
      [:pre (with-out-str (pprint @id*))]]
     [:div.delegation
      [:h3 "@delegation*"]
      [:pre (with-out-str (pprint @delegation*))]]
     [:div.delegation-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

;; delegation components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tabs [active]
  [:> react-bootstrap/Nav {:className "mb-3"
                           :justify false
                           :variant "tabs"
                           :defaultActiveKey active}
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link
     (let [href (path :inventory-pool-delegation
                      {:inventory-pool-id @inventory-pool/id*
                       :delegation-id @id*})]
       {:active (= (:path @routing/state*) href)
        :href href})
     "Delegation"]]
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link
     (let [href (path :inventory-pool-delegation-users
                      {:inventory-pool-id @inventory-pool/id*
                       :delegation-id @id*})]
       {:active (= (:path @routing/state*) href)
        :href href})
     "Users"]]
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link
     (let [href (path :inventory-pool-delegation-groups
                      {:inventory-pool-id @inventory-pool/id*
                       :delegation-id @id*})]
       {:active (= (:path @routing/state*) href)
        :href href})
     "Groups "]]])

(defn delegation-name []
  [:<>
   [routing/hidden-state-component
    {:did-change fetch}]
   (let [inner (when-let [dname (some-> @data* (get @id*) :name)]
                 [:<> dname])]
     [:<> inner])])

(defn name-link-component []
  [:span.delegation-name
   [routing/hidden-state-component
    {:did-change fetch}]
   (let [inner (if-let [dname (some-> @data* (get @id*) :name)]
                 [:em dname]
                 [:span {:style {:font-family "monospace"}} (short-id @id*)])
         delegation-path (path :inventory-pool-delegation
                               {:inventory-pool-id @inventory-pool/id*
                                :delegation-id @id*})]
     [link inner delegation-path])])

(defn header []
  [:header.mb-5
   [breadcrumbs/main]
   [:h1.mt-3 [delegation-name]]
   [:h6 "Inventory Pool " [inventory-pool/name-component]]])
