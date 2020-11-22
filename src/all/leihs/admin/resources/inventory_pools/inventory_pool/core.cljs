(ns leihs.admin.resources.inventory-pools.inventory-pool.core
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

    [leihs.admin.resources.inventory-pools.inventory-pool.breadcrumbs :as breadcrumbs]
    [leihs.admin.common.components :as components]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :inventory-pool-id presence)
                ":inventory-pool-id")))

(defonce data* (reagent/atom nil))

(defonce role-for-inventory-pool* (reaction
                                    (some->> @core-user/state* :access-rights
                                             (filter #(=(:inventory_pool_id %) @id*))
                                             first
                                             :role
                                             keyword)))

(defonce edit-mode?*
  (reaction
    (and (map? @data*)
         (boolean ((set '(:inventory-pool-edit :inventory-pool-create))
                   (:handler-key @routing/state*))))))


;;; fetch ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch []
  (defonce fetch-id* (reagent/atom nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Inventory-Pool"
                               :handler-key :inventory-pool
                               :retry-fn #'fetch}
                              :chan resp-chan)]
    (reset! fetch-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-id*))
            (reset! data* (:body resp)))))))


(defn clean-and-fetch [& args]
  (reset! data* nil)
  (fetch))


;;; debug ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-component []
  (when (:debug @state/global-state*)
    [:div.inventory-pool-debug
     [:hr]
     [:div.inventory-pool-data
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))


;;; components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn name-link-component []
  [:span
   [routing/hidden-state-component
    {:did-change fetch}]
   (let [p (path :inventory-pool {:inventory-pool-id @id*})
         inner (if @data*
                 [:em (str (:name @data*))]
                 [:span {:style {:font-family "monospace"}} (short-id @id*)])]
     [components/link inner p])])


(defn link-to-legacy-component []
  [:div
   (when (= @role-for-inventory-pool* :inventory_manager)
     [:a {:href (str "/manage/inventory_pools/" @id* "/edit")}
      "Edit " [name-link-component]
      " in the leihs-legacy interface. "])])



