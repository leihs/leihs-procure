(ns leihs.admin.resources.inventory-pools.inventory-pool.fields.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.components :refer [toggle-component]]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool.core]
   [leihs.admin.resources.inventory-pools.inventory-pool.fields.core :as core]
   [leihs.admin.resources.inventory-pools.inventory-pool.fields.edit :as edit]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as react-bootstrap :refer [Alert]]))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:<>
     [:div.inventory-pool-fields-debug
      [:hr]
      [:div.inventory-pool-fields-data
       [:h3 "@core/data*"]
       [:pre (with-out-str (pprint @core/data*))]]]]))

(defn core-table-component [fields]
  (if-let [fields (seq fields)]
    [table/container
     {:className "fields"
      :header [:tr
               [:th "Index"]
               [:th "ID"]
               [:th "Label"]
               [:th "Required?"]
               [:th "Target Type"]
               [:th "Status"]]
      :body (doall
             (for [[index {:keys [id data is_disabled]}] (map-indexed vector fields)]
               ^{:key id}
               [:tr.field
                [:td (inc index)]
                [:td id]
                [:td (-> data :label)]
                [:td (when (-> data :required) "*")]
                [:td (-> data :target_type core/format-target-type)]
                [:td (let [enabled? (not is_disabled)]
                       [:span.text-nowrap (toggle-component enabled?) " " (if enabled? "Enabled" "Disabled")])]]))}]
    [:> Alert {:variant "info"
               :className "text-center"}
     "No fields found."]))

(defn fields-component []
  (let [fields @core/data*]
    (if-not fields
      [wait-component]
      [:<>
       [:div {:style {:max-width "1000px"}}
        [core-table-component fields]]
       [edit/button]
       [edit/dialog]])))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount #(do (inventory-pool.core/fetch)
                     (core/clean-and-fetch))}]

   [:article.inventory-pool-fields
    [inventory-pool.core/header]

    [:section.mb-5
     [inventory-pool.core/tabs]
     [fields-component]]

    [debug-component]]])
