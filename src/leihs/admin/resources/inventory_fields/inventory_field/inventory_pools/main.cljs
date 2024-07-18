(ns leihs.admin.resources.inventory-fields.inventory-field.inventory-pools.main
  (:require
   [leihs.admin.common.components :refer [toggle-component]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.resources.inventory-fields.inventory-field.inventory-pools.core :as core]
   [leihs.admin.resources.inventory-fields.inventory-field.inventory-pools.edit :as edit]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]))

(defn component []
  [:<>
   [routing/hidden-state-component
    {:did-mount core/clean-and-fetch}]

   (if-let [pools @core/data*]
     [:div#inventory-field-inventory-pools
      [:h3 "Field status by inventory pool"]
      [table/container
       {:borders false
        :header [:tr
                 [:th "Index"]
                 [:th "Short name"]
                 [:th "Inventory pool"]
                 [:th "Field status"]]
        :body (doall (for [[index {:keys [id shortname name is_disabled]}] (map-indexed vector pools)]
                       ^{:key id}
                       [:tr.pool-row
                        [:td (inc index)]
                        [:td shortname]
                        [:td name]
                        [:td
                         (let [enabled? (not is_disabled)]
                           [:span.text-nowrap (toggle-component enabled?) " " (if enabled? "Enabled" "Disabled")])]]))}]
      [edit/button]
      [edit/dialog]]
     [wait-component])])
