(ns leihs.admin.resources.inventory-pools.inventory-pool.core
  (:require
   [cljs.pprint :refer [pprint]]
   [clojure.string :refer [join]]
   [leihs.admin.common.components :as components]
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [presence]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.shared :refer [short-id]]
   [react-bootstrap :as react-bootstrap :refer [Nav]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :inventory-pool-id presence)
                ":inventory-pool-id")))

(def path*
  (reaction
   (path :inventory-pool {:inventory-pool-id @id*})))

(def cache* (reagent/atom nil))

(def data*
  (reaction
   (get @cache* @path*)))

(defn fetch []
  (http-client/route-cached-fetch cache* {:route @path*}))

;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.inventory-pool-debug
     [:hr]
     [:div.inventory-pool-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; shared tabs for main view
(defn tabs [active]
  [:> Nav {:className "mb-3"
           :justify false
           :variant "tabs"
           :defaultActiveKey active}
   [:> Nav.Item
    [:> Nav.Link
     {:href (join ["/admin/inventory-pools/" @id*])}
     [icons/inventory-pools]
     " Settings "]]
   [:> Nav.Item
    [:> Nav.Link
     (let [href (path :inventory-pool-opening-times
                      {:inventory-pool-id @id*})]
       {:active (clojure.string/includes? (:path @routing/state*) href)
        :href href})
     [icons/opening-times]
     " Opening-Times "]]
   [:> Nav.Item
    [:> Nav.Link
     (let [href (path :inventory-pool-users
                      {:inventory-pool-id @id*})]
       {:active (clojure.string/includes? (:path @routing/state*) href)
        :href href})
     [icons/users]
     " Users "]]

   [:> Nav.Item
    [:> Nav.Link
     (let [href (path :inventory-pool-groups
                      {:inventory-pool-id @id*})]
       {:active (clojure.string/includes? (:path @routing/state*) href)
        :href href})
     [icons/groups]
     " Groups "]]

   [:> Nav.Item
    [:> Nav.Link
     (let [href (path :inventory-pool-delegations
                      {:inventory-pool-id @id*})]
       {:active (clojure.string/includes? (:path @routing/state*) href)
        :href href})
     [icons/delegations]
     " Delegations "]]

   [:> Nav.Item
    [:> Nav.Link
     (let [href (path :inventory-pool-entitlement-groups
                      {:inventory-pool-id @id*})]
       {:active (clojure.string/includes? (:path @routing/state*) href)
        :href href})
     [icons/award]
     " Entitlement-Groups "]]])

(defn name-component []
  [:<>
   [routing/hidden-state-component
    {:did-change fetch}]

   (let [inner (when @data*
                 [:<> (str (:name @data*))])]
     [:<> inner])])

(defn pool-name []
  (let [name (:name @data*)]
    (fn []
      [:h1 name])))

(defn header []
  (if-not @data*
    [:div.my-5
     [wait-component]]
    [:header.my-5
     [breadcrumbs/main]
     [pool-name]]))

(defn name-link-component []
  [:span
   [routing/hidden-state-component
    {:did-mount #(fetch)}]

   (let [p (path :inventory-pool {:inventory-pool-id @id*})
         inner (if @data*
                 [:em (str (:name @data*))]
                 [:span {:style {:font-family "monospace"}} (short-id @id*)])]
     [components/link inner p])])
