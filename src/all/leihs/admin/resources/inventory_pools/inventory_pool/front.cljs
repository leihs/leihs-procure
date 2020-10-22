(ns leihs.admin.resources.inventory-pools.inventory-pool.front
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

    [leihs.admin.front.breadcrumbs :as breadcrumbs]
    [leihs.admin.front.components :as components :refer [field-component checkbox-component]]
    [leihs.core.icons :as icons]
    [leihs.admin.front.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.core.async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]
    ))



(defonce inventory-pool-id* (reaction (-> @routing/state* :route-params :inventory-pool-id)))
(defonce inventory-pool-data* (reagent/atom nil))
(defonce role-for-inventory-pool* (reaction
                                    (some->> @core-user/state* :access-rights
                                             (filter #(=(:inventory_pool_id %) @inventory-pool-id*))
                                             first
                                             :role
                                             keyword)))

(defonce edit-mode?*
  (reaction
    (and (map? @inventory-pool-data*)
         (boolean ((set '(:inventory-pool-edit :inventory-pool-add))
                   (:handler-key @routing/state*))))))




;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def fetch-inventory-pool-id* (reagent/atom nil))
(defn fetch-inventory-pool []
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Inventory-Pool"
                               :handler-key :inventory-pool
                               :retry-fn #'fetch-inventory-pool}
                              :chan resp-chan)]
    (reset! fetch-inventory-pool-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-inventory-pool-id*))
            (reset! inventory-pool-data* (:body resp)))))))


(defn clean-and-fetch [& args]
  (reset! inventory-pool-data* nil)
  (fetch-inventory-pool))


;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.inventory-pool-debug
     [:hr]
     [:div.inventory-pool-data
      [:h3 "@inventory-pool-data*"]
      [:pre (with-out-str (pprint @inventory-pool-data*))]]]))


;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn name-component []
  [:span
   [routing/hidden-state-component
    {:did-mount fetch-inventory-pool
     :did-change fetch-inventory-pool}]
   (if @inventory-pool-data*
     [:em (str (:name @inventory-pool-data*))]
     [:span {:style {:font-family "monospace"}} (short-id @inventory-pool-id*)])])

(defn inventory-pool-component []
  [:div.inventory-pool
   [field-component [:name] inventory-pool-data* edit-mode?* {}]
   [field-component [:shortname] inventory-pool-data* edit-mode?* {}]
   [checkbox-component [:is_active] inventory-pool-data* edit-mode?*]
   [field-component [:email] inventory-pool-data* edit-mode?* {:type :email}]
   [field-component [:description] inventory-pool-data* edit-mode?* {:node-type :textarea}]])

;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool {:inventory-pool-id @inventory-pool-id*})
                               :method :patch
                               :json-params  @inventory-pool-data*}
                              {:modal true
                               :title "Update Inventory-Pool"
                               :handler-key :inventory-pool-edit
                               :retry-fn #'patch}
                              :chan resp-chan)]
    (go (let [resp (<! resp-chan)]
          (when (= (:status resp) 204)
            (accountant/navigate!
              (path :inventory-pool {:inventory-pool-id @inventory-pool-id*})))))))

(defn patch-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-warning
       {:on-click patch}
       [:i.fas.fa-save]
       " Save "]]
     [:div.clearfix]]))

(defn edit-page []
  [:div.edit-inventory-pool
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/inventory-pools-li)
      (breadcrumbs/inventory-pool-li @inventory-pool-id*)
      (breadcrumbs/inventory-pool-edit-li @inventory-pool-id*)][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Inventory-Pool "]
      [name-component]]]]
   [:div.form
    [inventory-pool-component]
    [patch-submit-component]]
   [debug-component]])


;;; add  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [_]
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pools)
                               :method :post
                               :json-params  @inventory-pool-data*}
                              {:modal true
                               :title "Add Inventory-Pool"
                               :handler-key :inventory-pool-add
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
       " Add "]]
     [:div.clearfix]]))

(defn add-page []
  [:div.new-inventory-pool
   [routing/hidden-state-component
    {:did-mount #(reset! inventory-pool-data* {})}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/inventory-pools-li)
      (breadcrumbs/inventory-pool-add-li)
      ][])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Add Inventory-Pool "]]]]
   [inventory-pool-component]
   [create-submit-component]
   [debug-component]])


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
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   [:div.row
    [:nav.col-lg {:aria-label :breadcrumb :role :navigation}
     [:ol.breadcrumb
      [breadcrumbs/leihs-li]
      [breadcrumbs/admin-li]
      [breadcrumbs/inventory-pools-li]
      [breadcrumbs/inventory-pool-li @inventory-pool-id*]
      [breadcrumbs/inventory-pool-delete-li @inventory-pool-id*]]]
    [:nav.col-lg {:role :navigation}]]
   [:h1 "Delete Inventory-Pool "
    [name-component]]
   [delete-submit-component]])


(defn link-to-legacy-component []
  [:div
   (when (= @role-for-inventory-pool* :inventory_manager)
     [:p
      (when true
        [:a {:href (str "/manage/inventory_pools/" @inventory-pool-id* "/edit")}
         "Edit " [name-component]
         " in the leihs-legacy interface. "])])])

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.inventory-pool
   [routing/hidden-state-component
    {:did-mount clean-and-fetch
     :did-change clean-and-fetch}]
   (breadcrumbs/nav-component
     [(breadcrumbs/leihs-li)
      (breadcrumbs/admin-li)
      (breadcrumbs/inventory-pools-li)
      (breadcrumbs/inventory-pool-li @inventory-pool-id*)]
     [(breadcrumbs/inventory-pool-delete-li @inventory-pool-id*)
      (breadcrumbs/inventory-pool-edit-li @inventory-pool-id*)
      (breadcrumbs/inventory-pool-users-li @inventory-pool-id*)
      (breadcrumbs/inventory-pool-groups-li @inventory-pool-id*)
      (breadcrumbs/inventory-pool-entitlement-groups-li @inventory-pool-id*)
      ])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Inventory-Pool "]
      [name-component]]]]
   [link-to-legacy-component]
   [inventory-pool-component]
   [debug-component]])
