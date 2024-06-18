(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core
  (:require
   [cljs.core.async :as async :refer [<! go timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.set :refer [rename-keys]]
   [leihs.admin.common.components :as components :refer [link]]
   [leihs.admin.common.components.navigation.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.state :as state]
   [leihs.core.routing.front :as routing]
   [leihs.core.user.front]
   [leihs.core.user.shared :refer [short-id]]
   [react-bootstrap :as react-bootstrap :refer [Nav Form]]
   [reagent.core :as reagent :refer [reaction]]))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :delegation-id)
                ":delegation-id")))

(defonce data* (reagent/atom nil))

(def delegation*
  (reaction
   (get @data* @id*)))

(defn fetch []
  (go (swap! data* assoc @id*
             (some-> {:url (path :inventory-pool-delegation
                                 (-> @routing/state* :route-params))
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success! :body))))

(defn merge-delegation-with-params [data*]
  (reset! data* (merge @delegation*
                       (rename-keys (dissoc (get @routing/state* :query-params) :action)
                                    {:user-uid :responsible_user_id}))))

(defn responsible-user-choose-component [data*]
  [:div.input-group-append
   [:a.btn.btn-primary
    {:tab-index form-components/TAB-INDEX
     :href (path :users-choose {}
                 {:return-to (path (:handler-key @routing/state*)
                                   (:route-params @routing/state*)
                                   (conj
                                    (:query-params @routing/state*)
                                    (select-keys @data* [:name
                                                         :responsible_user_id
                                                         :pool_protected])))})}

    [:i.fas.fa-rotate-90.fa-hand-pointer.px-2]
    " Choose responsible user "]])

(defn delegation-form [action data*]
  [:> Form {:id "delegation-form"
            :on-submit (fn [e]
                         (.preventDefault e)
                         (action))}
   [form-components/input-component data* [:name]
    :label "Name"]
   [form-components/input-component data* [:responsible_user_id]
    {:label "Responsible user"
     :append #(responsible-user-choose-component data*)}]
   [:div
    [form-components/checkbox-component data* [:pool_protected]
     :label "Protected"
     :hint [:span
            "An " [:strong " unprotected "]
            " data can be " [:strong "added"] " to any other pool and then be used and "
            [:strong  " modified "] " from those pools in every way."
            " You can unprotect a data temporarily to share it with a limited number of pools."]]]])

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
  [:> Nav {:className "mb-3"
           :justify false
           :variant "tabs"
           :defaultActiveKey active}
   [:> Nav.Item
    [:> Nav.Link
     (let [href (path :inventory-pool-delegation
                      {:inventory-pool-id @inventory-pool/id*
                       :delegation-id @id*})]
       {:active (= (:path @routing/state*) href)
        :href href})
     "Delegation"]]
   [:> Nav.Item
    [:> Nav.Link
     (let [href (path :inventory-pool-delegation-users
                      {:inventory-pool-id @inventory-pool/id*
                       :delegation-id @id*})]
       {:active (= (:path @routing/state*) href)
        :href href})
     "Users"]]
   [:> Nav.Item
    [:> Nav.Link
     (let [href (path :inventory-pool-delegation-groups
                      {:inventory-pool-id @inventory-pool/id*
                       :delegation-id @id*})]
       {:active (= (:path @routing/state*) href)
        :href href})
     "Groups "]]])

(defn delegation-name []
  [:<>
   [routing/hidden-state-component
    {:did-change #(fetch)}]

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
