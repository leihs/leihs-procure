(ns leihs.admin.resources.suppliers.supplier.items
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.core.async :as async :refer [<! go]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :as string]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.groups.group.core :as group.shared]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [str]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Alert]]
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

(defn clean-and-fetch []
  (reset! data* nil)
  (fetch-items))

#_(defn items-debug-component []
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
  [:div.mt-5
   [routing/hidden-state-component
    {:did-change clean-and-fetch}]
   (cond
     (nil? @data*) [:<>
                    [:h1.my-5 "Items"]
                    [wait-component "Loading Items ..."]]
     (empty? @data*) [:> Alert {:variant "info"
                                :className "text-center"}
                      "No Items"]
     :else [:<>
            [:h1.my-3 "Items"]
            [table/container
             {:className "items"
              :borders false
              :header [:tr [:th "Inventory-Pool"] [:th "Inventory-Code"] [:th "Model-Name"]]
              :body
              (for [row @data*]
                [:tr.item {:key (:inventory_code row)}
                 [:td
                  [:a {:href (path :inventory-pool
                                   {:inventory-pool-id (:inventory_pool_id row)})}
                   (:inventory_pool_name row)]]
                 [:td
                  [:a {:href (item-url row)} (:inventory_code row)]]
                 [:td
                  [:a {:href (model-url row)} (model-name row)]]])}]])])

