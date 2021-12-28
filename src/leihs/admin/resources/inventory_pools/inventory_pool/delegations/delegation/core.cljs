(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.core.user.front]

    [leihs.admin.common.components :as components :refer [link]]
    [leihs.admin.common.http-client.core :as http-client]
    [leihs.admin.utils.misc :refer [wait-component]]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as delegation-users]
    [leihs.admin.utils.regex :as regex]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))

(defonce id* (reaction (or (-> @routing/state* :route-params :delegation-id)
                           ":delegation-id")))

(defonce data* (reagent/atom nil))

(defonce delegation* (reaction (get @data* @id*)))

(defn fetch-delegation []
  (go (swap! data* assoc @id*
             (some-> {:url (path :inventory-pool-delegation
                                 (-> @routing/state* :route-params))
                      :chan (async/chan)}
                     http-client/request :chan <!
                     http-client/filter-success! :body))))


;;; reload logic ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch [_]
  (fetch-delegation))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn debug-component []
	(when (:debug @state/global-state*)
		[:div.delegation-debug
		 [:hr]
     [:div.delegation-id
      [:h3 "@id*"]
      [:pre (with-out-str (pprint @id*))]]
     [:div.delegation
      [:h3 "@delegation*"]
      [:pre (with-out-str (pprint @delegation*))]]
		 [:div.delegation-data
			[:h3 "@data*"]
			[:pre (with-out-str (pprint @data*))]]
		 ]))


;; delegation components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn name-link-component []
  [:span.delegation-name
   [routing/hidden-state-component
    {:did-change fetch}]
   (let [inner (if-let [dname (some-> @data* (get @id*) :name)]
                 [:em dname]
                 [:span {:style {:font-family "monospace"}} (short-id @id*)])
         delegation-path (path :inventory-pool-delegation
                               {:inventory-pool-id @inventory-pool/id*
                                :delegation-id @id*})]
     [link inner delegation-path])])

(defn delegation-id-component []
  [:p "delegation id: " [:span {:style {:font-family "monospace"}} (:id @data*)]])

