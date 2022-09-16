(ns leihs.admin.resources.suppliers.supplier.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.front :as core-user]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.admin.common.icons :as icons]

    [leihs.admin.common.components :as components]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.suppliers.supplier.breadcrumbs :as breadcrumbs]
    [leihs.admin.state :as state]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :supplier-id presence)
                ":supplier-id")))

(defonce data* (reagent/atom nil))

(defonce role-for-supplier* (reaction
                                    (some->> @core-user/state* :access-rights
                                             (filter #(=(:inventory_pool_id %) @id*))
                                             first
                                             :role
                                             keyword)))

(defonce edit-mode?*
  (reaction
    (and (map? @data*)
         (boolean ((set '(:supplier-edit :supplier-create))
                   (:handler-key @routing/state*))))))


;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch []
  (go (reset! data*
              (some->
                {:chan (async/chan)
                 :url (path :supplier
                            (-> @routing/state* :route-params))}
                http-client/request :chan <!
                http-client/filter-success! :body))))

(defn clean-and-fetch [& args]
  (reset! data* nil)
  (fetch))


;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.supplier-debug
     [:hr]
     [:div.supplier-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))


;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn id-component []
  [:p "id: " [:span {:style {:font-family "monospace"}} (:id @data*)]])

(defn name-link-component []
  [:span
   [routing/hidden-state-component
    {:did-change fetch}]
   (let [p (path :supplier {:supplier-id @id*})
         inner (if @data*
                 [:em (str (:name @data*))]
                 [:span {:style {:font-family "monospace"}} (short-id @id*)])]
     [components/link inner p])])
