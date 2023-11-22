(ns leihs.admin.resources.inventory-pools.inventory-pool.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [clojure.core :as core]
   [clojure.string :as str]
   [leihs.admin.common.components.table :as table]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.resources.inventory-pools.authorization :as pool-auth]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.delete :as delete]
   [leihs.admin.resources.inventory-pools.inventory-pool.edit :as edit]
   [leihs.admin.resources.inventory-pools.inventory-pool.nav :as nav]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.auth.core :as auth]
   [leihs.core.routing.front :as routing]
   [react-bootstrap :as BS :refer [Button]]
   [reagent.core :as reagent]))

;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn edit-inventory-pool []
  (when (auth/allowed? [pool-auth/pool-inventory-manager?
                        auth/admin-scopes?])
    (let [show (reagent/atom false)]
      (fn []
        [:<>
         [:> Button
          {:className ""
           :onClick #(reset! show true)}
          "Edit"]
         [edit/dialog {:show @show
                       :onHide #(reset! show false)}]]))))

(defn delete-inventory-pool []
  (when (auth/allowed? [auth/admin-scopes?])
    (let [show (reagent/atom false)]
      (fn []
        [:<>
         [:> Button
          {:className "ml-3"
           :variant "danger"
           :onClick #(reset! show true)}
          "Delete"]
         [delete/dialog {:show @show
                         :onHide #(reset! show false)}]]))))

(defn inventory-pool-info-table []
  (if-not @inventory-pool/data*
    [wait-component]
    [table/container
     {:borders false
      :header [:tr [:th "Property"] [:th.w-75 "Value"]]
      :body
      [:<>
       [:tr.active
        [:td "Active" [:small " (is_active)"]]
        [:td.active (core/str  (:is_active @inventory-pool/data*))]]
       [:tr.shortname
        [:td "Short Name" [:small " (shortname)"]]
        [:td.shortname (:shortname @inventory-pool/data*)]]
       [:tr.name
        [:td "Name" [:small " (name)"]]
        [:td.name
         (:name @inventory-pool/data*)]]
       [:tr.email
        [:td "Email" [:small " (email)"]]
        [:td.email (:email @inventory-pool/data*)]]
       [:tr.description
        [:td "Description" [:small " (description)"]]
        [:td.description
         {:style {:white-space "break-spaces"}}
         (:description @inventory-pool/data*)]]]}]))

(defn page []
  [:<>
   [routing/hidden-state-component
    {:did-mount inventory-pool/clean-and-fetch}]
   (if-not @inventory-pool/data*
     [:div.my-5
      [wait-component " Loading Data ..."]]
     [:article.inventory-pool.my-5
      [:h1.my-5
       [inventory-pool/name-component]]
      [nav/tabs (str/join ["/admin/inventory-pools/" @inventory-pool/id*])]
      [inventory-pool-info-table]
      [edit-inventory-pool]
      [delete-inventory-pool]
      [inventory-pool/debug-component]])])
