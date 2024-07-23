(ns leihs.admin.resources.suppliers.supplier.items
  (:require
   [clojure.string :as string]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.suppliers.supplier.core :as core]
   [leihs.admin.utils.misc :refer [wait-component]]
   [react-bootstrap :as react-bootstrap :refer [Alert]]))

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
  [:<>
   (cond
     (nil? @core/data-items*)
     [:<>
      [:h1.my-5 "Items"]
      [wait-component]]

     (empty? @core/data-items*)
     [:> Alert {:variant "info"
                :className "text-center"}
      "No Items"]

     :else [:<>
            [:h1.my-3 "Items"]
            [table/container
             {:className "items"
              :borders false
              :header [:tr [:th "Inventory Pool"] [:th "Inventory Code"] [:th "Model Name"]]
              :body
              (for [row @core/data-items*]
                [:tr.item {:key (:inventory_code row)}
                 [:td
                  [:a {:href (path :inventory-pool
                                   {:inventory-pool-id (:inventory_pool_id row)})}
                   (:inventory_pool_name row)]]
                 [:td
                  [:a {:href (item-url row)} (:inventory_code row)]]
                 [:td
                  [:a {:href (model-url row)} (model-name row)]]])}]])])
