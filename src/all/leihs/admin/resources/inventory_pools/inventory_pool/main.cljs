(ns leihs.admin.resources.inventory-pools.inventory-pool.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.core.icons :as icons]

    [leihs.admin.common.form-components :as form-components]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.breadcrumbs :as breadcrumbs]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))

(defonce edit-mode?*
  (reaction
    (and (map? @inventory-pool/data*)
         (boolean ((set '(:inventory-pool-edit :inventory-pool-create))
                   (:handler-key @routing/state*))))))

;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inventory-pool-component []
  (if-not @inventory-pool/data*
    [wait-component]
    [:div.inventory-pool.mt-3
     [:div.mb-3
      [form-components/checkbox-component inventory-pool/data* [:is_active]
       :label "Active"
       :disabled (not @edit-mode?*)]]
     [:div
      [form-components/input-component inventory-pool/data* [:shortname]
       :label "Short name"
       :disabled (not @edit-mode?*)]]
     [:div
      [form-components/input-component inventory-pool/data* [:name]
       :label "Name"
       :disabled (not @edit-mode?*)]]
     [:div
      [form-components/input-component inventory-pool/data* [:email]
       :label "Email"
       :type :email
       :disabled (not @edit-mode?*)]]
     [form-components/input-component inventory-pool/data* [:description]
      :label "Description"
      :element :textarea
      :rows 20
      :disabled (not @edit-mode?*)]]))


;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool {:inventory-pool-id @inventory-pool/id*})
                               :method :patch
                               :json-params  @inventory-pool/data*}
                              {:modal true
                               :title "Update Inventory-Pool"
                               :handler-key :inventory-pool-edit
                               :retry-fn #'patch}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :inventory-pool {:inventory-pool-id @inventory-pool/id*})))))))

(defn edit-page []
  [:div.edit-inventory-pool
   [routing/hidden-state-component
    {:did-mount inventory-pool/clean-and-fetch}]
   (breadcrumbs/nav-component
     (conj @breadcrumbs/left*
           [breadcrumbs/edit-li])[])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Inventory-Pool "]
      [inventory-pool/name-link-component]]]]
   [:form.form
    {:on-submit (fn [e] (.preventDefault e) (patch))}
    [inventory-pool-component]
    [form-components/save-submit-component]]
   [inventory-pool/debug-component]])


;;; add  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pools)
                               :method :post
                               :json-params  @inventory-pool/data*}
                              {:modal true
                               :title "Add Inventory-Pool"
                               :handler-key :inventory-pool-create
                               :retry-fn #'create}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 200)
            (accountant/navigate!
              (path :inventory-pool {:inventory-pool-id (-> resp :body :id)})))))))

(defn create-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-primary
       {:on-click create}
       " Create "]]
     [:div.clearfix]]))

(defn create-page []
  [:div.new-inventory-pool
   [routing/hidden-state-component
    {:did-mount #(reset! inventory-pool/data* {})}]
   (breadcrumbs/nav-component
     (conj @breadcrumbs/left*
           [breadcrumbs/create-li])
     [])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Create Inventory-Pool "]]]]
   [inventory-pool-component]
   [create-submit-component]
   [inventory-pool/debug-component]])


;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn delete-inventory-pool [& args]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool (-> @routing/state* :route-params))
                               :method :delete
                               :query-params {}}
                              {:title "Delete Inventory-Pool"
                               :handler-key :inventory-pool-delete
                               :retry-fn #'delete-inventory-pool}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :inventory-pools {}
                    (-> @state/global-state* :inventory-pools-query-params))))))))

(defn delete-submit-component []
  [:div.form
   [:div.float-right
    [:button.btn.btn-warning
     {:on-click delete-inventory-pool}
     icons/delete
     " Delete "]]
   [:div.clearfix]])

(defn delete-page []
  [:div.inventory-pool-delete
   [routing/hidden-state-component
    {:did-mount inventory-pool/clean-and-fetch}]
   [:div.row
    (breadcrumbs/nav-component
      (conj @breadcrumbs/left*
            [breadcrumbs/delete-li])
      [])
    [:nav.col-lg {:role :navigation}]]
   [:h1 "Delete Inventory-Pool "
    [inventory-pool/name-link-component]]
   [delete-submit-component]])


;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.inventory-pool
   [routing/hidden-state-component
    {:did-mount inventory-pool/clean-and-fetch}]
   [breadcrumbs/nav-component
     @breadcrumbs/left*
     [[breadcrumbs/users-li]
      [breadcrumbs/groups-li]
      [breadcrumbs/delegations-li]
      [breadcrumbs/entitlement-groups-li]
      [breadcrumbs/delete-li]
      [breadcrumbs/edit-li]]]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Inventory-Pool "]
      [inventory-pool/name-link-component]]]]
   [inventory-pool/link-to-legacy-component]
   [inventory-pool-component]
   [inventory-pool/debug-component]])
