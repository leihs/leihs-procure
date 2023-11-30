(ns leihs.admin.resources.suppliers.supplier.items
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :as string]
   [leihs.admin.common.breadcrumbs :as breadcrumbs]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.roles.core :as roles]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.group.core :as group.shared :refer [group-id*]]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

(defonce data* (reagent/atom nil))

(defn fetch-items []
  (go (reset! data*
              (some->
               {:chan (async/chan)
                :url (path :supplier-items
                           (-> @routing/state* :route-params))}
               http-client/request :chan <!
               http-client/filter-success!
               :body :items))))

(defn clean-and-fetch [& args]
  (reset! data* nil)
  (fetch-items))

(defn items-debug-component []
  [:div
   (when (:debug @state/global-state*)
     [:div.inventory-pools-roles-debug
      [:hr]
      [:div.inventory-pools-roles-data
       [:h3 "@data*"]
       [:pre (with-out-str (pprint @data*))]]])
   [group.shared/debug-component]])

(defn model-name [row]
  (-> row
      (select-keys [:product :version])
      vals
      (->> (filter identity)
           (string/join " "))))

(defn item-url [row]
  (str "/manage/" (:inventory_pool_id row) "/items/" (:id row) "/edit"))

(defn model-url [row]
  (str "/manage/" (:inventory_pool_id row) "/models/" (:model_id row) "/edit"))

(defn component []
  [:div
   [routing/hidden-state-component
    {:did-change clean-and-fetch}]
   (cond
     (nil? @data*) [:<> [:h2 "Items"] [wait-component]]
     (empty? @data*) [:h2 "No Items"]
     :else [:<>
            [:h2 "Items"]
            [:table.roles.table
             [:thead
              [:tr [:th "Inventory-Pool"] [:th "Inventory-Code"] [:th "Model-Name"]]]
             [:tbody
              (for [row @data*]
                [:tr.item {:key (:inventory_pool_name row)}
                 [:td
                  [:a {:href (path :inventory-pool
                                   {:inventory-pool-id (:inventory_pool_id row)})}
                   [:em (:inventory_pool_name row)]] ""]
                 [:td
                  [:a {:href (item-url row)} (:inventory_code row)]]
                 [:td
                  [:a {:href (model-url row)} (model-name row)]]])]]])])
