(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [cljs.core.async :as async]
   [leihs.admin.common.components :as components]
   [leihs.admin.common.components.navigation.back :as back]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.core.core :refer [presence str]]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.shared :refer [short-id]]
   [react-bootstrap :as react-bootstrap]
   [reagent.core :as reagent]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :entitlement-group-id presence)
                ":entitlement-group-id")))

(defonce data* (reagent/atom nil))

(defonce path*
  (reaction
   (path :inventory-pool-entitlement-group
         {:inventory-pool-id @inventory-pool/id*
          :entitlement-group-id @id*})))

(defn fetch []
  (go (reset! data*
              (some->
               {:url @path*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success! :body))))

(defn clean-and-fetch [& _]
  (reset! data* nil)
  (fetch))

(defn entitlement-group-name []
  [:<>
   [routing/hidden-state-component
    {:did-change fetch}]
   (let [inner (when @data*
                 [:<> (str (:name @data*))])]
     [:<> inner])])

(defn tabs [active]
  [:> react-bootstrap/Nav {:className "mb-3"
                           :justify false
                           :variant "tabs"
                           :defaultActiveKey active}
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link

     (let [href (path :inventory-pool-entitlement-group
                      {:inventory-pool-id @inventory-pool/id*
                       :entitlement-group-id @id*})]
       {:active (= (:path @routing/state*) href)
        :href href})
     "Overview"]]
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link
     (let [href (path :inventory-pool-entitlement-group-users
                      {:inventory-pool-id @inventory-pool/id*
                       :entitlement-group-id @id*})]
       {:active (= (:path @routing/state*) href)
        :href href})
     "Users"]]
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link
     (let [href (path :inventory-pool-entitlement-group-groups
                      {:inventory-pool-id @inventory-pool/id*
                       :entitlement-group-id @id*})]
       {:active (= (:path @routing/state*) href)
        :href href})
     "Groups"]]])

(defn header []
  [:header.my-5
   [back/button {:href (path :inventory-pool-entitlement-groups
                             {:inventory-pool-id @inventory-pool/id*})}]
   [:h1.mt-3 [entitlement-group-name]]
   [:h6 "Inventory Pool " [inventory-pool/name-component]]])
