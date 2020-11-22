(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]
    [leihs.core.user.shared :refer [short-id]]
    [leihs.core.user.front]

    [leihs.admin.common.components :as components :refer [link]]
    [leihs.admin.utils.misc :refer [humanize-datetime-component wait-component]]
    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
    [leihs.admin.resources.inventory-pools.inventory-pool.users.main :as delegation-users]
    [leihs.admin.utils.regex :as regex]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [cljsjs.jimp]
    [cljsjs.moment]
    [clojure.contrib.inflect :refer [pluralize-noun]]
    [reagent.core :as reagent]))

(declare fetch-resposible-user responsible-user-data*)

(defonce id* (reaction (or (-> @routing/state* :route-params :delegation-id)
                           ":delegation-id")))

(defonce data* (reagent/atom nil))

(defonce delegation* (reaction (get @data* @id*)))

(defn fetch-delegation []
  (def fetch-id* (reagent/atom nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :inventory-pool-delegation (-> @routing/state* :route-params))
                               :method :get
                               :query-params {}}
                              {:modal false
                               :title "Fetch Delegation"
                               :handler-key :delegation}
                              :chan resp-chan)
        delegation-id-of-request @id*]

    (reset! fetch-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200)
                     (= id @fetch-id*))
            (swap! data* assoc delegation-id-of-request (:body resp))
            ;(reset! responsible-user-data* nil)
            ;(fetch-resposible-user)
            )))))


;;; responsible user ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def responsible-user-id*
	(reaction
		(re-matches regex/uuid-pattern
								(or (-> @data* :responsible_user_id presence)
										""))))

(def responsible-user-data* (reagent/atom nil))

(def fetch-resposible-user-id* (reagent/atom nil))
(defn fetch-resposible-user []
	(when-let [id @responsible-user-id*]
		(let [resp-chan (async/chan)
					id (requests/send-off {:url (path :user {:user-id id})
																 :method :get
																 :query-params {}}
																{:modal false
																 :title "Fetch Responsible User"
																 :handler-key :delegation
																 :retry-fn #'fetch-resposible-user}
																:chan resp-chan)]
			(reset! fetch-resposible-user-id* id)
			(go (let [resp (<! resp-chan)]
						(when (and (= (:status resp) 200)
											 (= id @fetch-resposible-user-id*))
							(reset! responsible-user-data* (:body resp))))))))

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
		 [:div.resonsible-user-id
			[:h3 "@responsible-user-id*"]
			[:pre (with-out-str (pprint @responsible-user-id*))]]
		 [:div.resonsible-user-data
			[:h3 "@responsible-user-data*"]
			[:pre (with-out-str (pprint @responsible-user-data*))]]]))


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

