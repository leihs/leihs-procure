(ns leihs.admin.resources.inventory-pools.inventory-pool.entitlement-groups.entitlement-group.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.core.icons :as icons]

    [leihs.admin.common.components :as components]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.users.main :as users]

    [accountant.core :as accountant]
    [cljs.core.async :as async]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(defonce id*
  (reaction (or (-> @routing/state* :route-params :entitlement-group-id presence)
                ":entitlement-group-id")))

(defonce data* (reagent/atom nil))

(defonce path*
  (reaction
    (path :inventory-pool-entitlement-group
          {:inventory-pool-id @inventory-pool/id*
           :entitlement-group-id @id*})))

(defn fetch []
  (def fetch-id* (reagent/atom nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url @path*
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Entitlement-group"
                               :handler-key :entitlement-group
                               :retry-fn #'fetch}
                              :chan resp-chan)]
    (reset! fetch-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-id*))
            (reset! data* (:body resp)))))))

(defn clean-and-fetch [& _]
  (reset! data* nil)
  (fetch))

(defn name-link-component []
  [:span
   [routing/hidden-state-component
    {:did-mount clean-and-fetch}]
   [components/link
    (if @data*
      [:em (str (:name @data*))]
      [:span {:style {:font-family "monospace"}} (short-id @id*)])
    @path*]])


