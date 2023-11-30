(ns leihs.admin.resources.buildings.building.main
  (:refer-clojure :exclude [str keyword])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [reagent.ratom :as ratom :refer [reaction]])
  (:require
   [accountant.core :as accountant]
   [cljs.core.async :as async]
   [cljs.core.async :refer [timeout]]
   [cljs.pprint :refer [pprint]]
   [clojure.contrib.inflect :refer [pluralize-noun]]
   [leihs.admin.common.form-components :as form-components]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.buildings.breadcrumbs :as breadcrumbs-parent]
   [leihs.admin.resources.buildings.building.breadcrumbs :as breadcrumbs]
   [leihs.admin.resources.buildings.building.core :as building :refer [clean-and-fetch id* data*]]
   [leihs.admin.state :as state]
   [leihs.admin.utils.misc :refer [wait-component]]
   [leihs.core.core :refer [keyword str presence]]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

(defonce edit-mode?*
  (reaction
   (and (map? @data*)
        (boolean ((set '(:building-edit :building-create))
                  (:handler-key @routing/state*))))))

;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn building-component []
  (if-not @building/data*
    [wait-component]
    [:div.building.mt-3
     (when (:is_general @building/data*)
       [:div.alert.alert-info "This is a general building which is used for unknown locations of items."])
     [:div
      [form-components/input-component building/data* [:name]
       :label "Name"
       :required true
       :disabled (not @building/edit-mode?*)]]
     [form-components/input-component building/data* [:code]
      :label "Code"
      :disabled (not @building/edit-mode?*)]]))

;;; edit ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch [& args]
  (let [route (path :building {:building-id @building/id*})]
    (go (when (some->
               {:url route
                :method :patch
                :json-params  @building/data*
                :chan (async/chan)}
               http-client/request :chan <!
               http-client/filter-success!)
          (accountant/navigate! route)))))

(defn edit-page []
  [:div.edit-building
   [routing/hidden-state-component
    {:did-mount building/clean-and-fetch}]
   (breadcrumbs/nav-component
    (conj @breadcrumbs/left*
          [breadcrumbs/edit-li]) [])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Edit Building "]
      [building/name-link-component]]]]
   [:form.form
    {:on-submit (fn [e] (.preventDefault e) (patch))}
    [building-component]
    [form-components/save-submit-component]]
   [building/debug-component]])

;;; add  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create []
  (go (when-let [id (some->
                     {:url (path :buildings)
                      :method :post
                      :json-params  @building/data*
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success!
                     :body :id)]
        (accountant/navigate!
         (path :building {:building-id id})))))

(defn create-submit-component []
  (if @edit-mode?*
    [:div
     [:div.float-right
      [:button.btn.btn-primary
       " Create "]]
     [:div.clearfix]]))

(defn create-page []
  [:div.new-building
   [routing/hidden-state-component
    {:did-mount #(reset! building/data* {})}]
   (breadcrumbs/nav-component
    (conj @breadcrumbs-parent/left*
          [breadcrumbs-parent/create-li])
    [])
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Create Building "]]]]
   [:form.form
    {:on-submit (fn [e] (.preventDefault e) (create))}
    [building-component]
    [create-submit-component]]
   [building/debug-component]])

;;; delete ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn delete-building [& args]
  (go (when (some->
             {:url (path :building (-> @routing/state* :route-params))
              :method :delete
              :chan (async/chan)}
             http-client/request :chan <!
             http-client/filter-success!)
        (accountant/navigate! (path :buildings)))))

(defn delete-form-component []
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (delete-building))}
   [form-components/delete-submit-component]])

(defn delete-page []
  [:div.group-delete
   [breadcrumbs/nav-component
    (conj  @breadcrumbs/left* [breadcrumbs/delete-li]) []]
   [:h1 "Delete Building "
    [building/name-link-component]]
   [building/id-component]
   [delete-form-component]])

;;; show ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn show-page []
  [:div.building
   [routing/hidden-state-component {:did-mount #(clean-and-fetch)}]
   [breadcrumbs/nav-component
    @breadcrumbs/left*
    [[breadcrumbs/buildings-li]
     [breadcrumbs/delete-li]
     [breadcrumbs/edit-li]]]
   [:div.row
    [:div.col-lg
     [:h1
      [:span " Building "]
      [building/name-link-component]]]]
   [building-component]
   [building/debug-component]])
