(ns leihs.admin.resources.inventory-pools.inventory-pool.nav
  (:require
   ["react-bootstrap" :as react-bootstrap]
   [clojure.string :as str]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.core.routing.front :as routing]))

(defn tabs [active]
  [:> react-bootstrap/Nav {:className "mb-3"
                           :justify false
                           :variant "tabs"
                           :defaultActiveKey active}
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link
     {:href (str/join ["/admin/inventory-pools/" @inventory-pool/id*])}
     [icons/inventory-pools]
     " Settings "]]
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link
     (let [href (path :inventory-pool-opening-times
                      {:inventory-pool-id @inventory-pool/id*})]
       {:active (clojure.string/includes? (:path @routing/state*) href)
        :href href})
     [icons/opening-times]
     " Opening Times "]]
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link
     (let [href (path :inventory-pool-users
                      {:inventory-pool-id @inventory-pool/id*})]
       {:active (clojure.string/includes? (:path @routing/state*) href)
        :href href})
     [icons/users]
     " Users "]]
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link
     (let [href (path :inventory-pool-groups
                      {:inventory-pool-id @inventory-pool/id*})]
       {:active (clojure.string/includes? (:path @routing/state*) href)
        :href href})
     [icons/groups]
     " Groups "]]
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link
     (let [href (path :inventory-pool-delegations
                      {:inventory-pool-id @inventory-pool/id*})]
       {:active (clojure.string/includes? (:path @routing/state*) href)
        :href href})
     [icons/delegations]
     " Delegations "]]
   [:> react-bootstrap/Nav.Item
    [:> react-bootstrap/Nav.Link
     (let [href (path :inventory-pool-entitlement-groups
                      {:inventory-pool-id @inventory-pool/id*})]
       {:active (clojure.string/includes? (:path @routing/state*) href)
        :href href})
     [icons/award]
     " Entitlement Groups "]]])
