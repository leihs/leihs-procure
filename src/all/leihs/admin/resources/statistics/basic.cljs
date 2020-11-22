(ns leihs.admin.resources.statistics.basic
  (:refer-clojure :exclude [str keyword])
  (:require-macros
    [reagent.ratom :as ratom :refer [reaction]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [leihs.core.core :refer [keyword str presence]]
    [leihs.core.requests.core :as requests]
    [leihs.core.routing.front :as routing]

    [leihs.admin.state :as state]
    [leihs.admin.paths :as paths :refer [path]]

    [accountant.core :as accountant]
    [cljs.core.async :as async :refer [timeout]]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]
    ))

(def data* (reagent/atom {}))

(defn fetch []
  (def fetch-id* (reagent/atom nil))
  (let [resp-chan (async/chan)
        id (requests/send-off {:url (path :statistics-basic)
                               :method :get}
                              {:modal false
                               :title "Basic Statistics" }
                              :chan resp-chan)]
    (reset! fetch-id* id)
    (go (let [resp (<! resp-chan)]
          (when (and (= (:status resp) 200) ;success
                     (= id @fetch-id*))
            (reset! data* (:body resp)))))))


(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))


(defn contracts-component []
  [:div.contracts
   [:h3 "Contracts overview "]
   [:small "A contract is active if it is associated with an hand out or return during the given period. "]
   [:table.table.table-striped
    [:thead
     [:tr
      [:th "Active contracts last 12 months"]
      [:th "Active contracts last 12 to 24 months"]
      [:th "Contracts total"]]]
    [:tbody
     [:tr
      [:td.active_contracts_0m_12m (:active_contracts_0m_12m_count @data*)]
      [:td.active_contracts_12m_24m (:active_contracts_12m_24m_count @data*)]
      [:td.contracts-total (:contracts_count @data*)]]]]])

(defn items-component []
  [:div.items
   [:h3 "Items overview "]
   [:small "An item is active if it was handed out or returned during the given period."]
   [:table.table.table-striped
    [:thead
     [:tr
      [:th "Active items last 12 months"]
      [:th "Active items last 12 to 24 months"]
      [:th "Items total"]]]
    [:tbody
     [:tr
      [:td.active_items_0m_12m (:active_items_0m_12m_count @data*)]
      [:td.active_items_12m_24m (:active_items_12m_24m_count @data*)]
      [:td.items-total (:items_count @data*)]]]]])


(defn models-component []
  [:div.models
   [:h3 "Models overview "]
   [:small "A model is active if it is associated with an item which was active during the given period."]
   [:table.table.table-striped
    [:thead
     [:tr
      [:th "Active models last 12 months"]
      [:th "Active models last 12 to 24 months"]
      [:th "Models total"]]]
    [:tbody
     [:tr
      [:td.active_models_0m_12m (:active_models_0m_12m_count @data*)]
      [:td.active_models_12m_24m (:active_models_12m_24m_count @data*)]
      [:td.models-total (:models_count @data*)]]]]])


(defn users-component []
  [:div.users
   [:h3 "Users overview"]
   [:small "An user account is active if it was used to sign in during the given period via an HTTP session. "]
   [:table.table.table-striped
    [:thead
     [:tr
      [:th "Active users last 12 months"]
      [:th "Active users last 12 to 24 months"]
      [:th "Users total"]]]
    [:tbody
     [:tr
      [:td.active_users_0m_12m (:active_users_0m_12m_count @data*)]
      [:td.active_users_12m_24m (:active_users_12m_24m_count @data*)]
      [:td.users-total (:users_count @data*)]]]]])

(defn pools-component []
  [:div.pools
   [:h3 "Pools overview "]
   [:small "A pool is active if it is associated with active contracts during the given period."]
   [:table.table.table-striped
    [:thead
     [:tr
      [:th "Active pools last 12 months"]
      [:th "Active pools last 12 to 24 months"]
      [:th "Pools total"]]]
    [:tbody
     [:tr
      [:td.active_pools_0m_12m (:active_pools_0m_12m_count @data*)]
      [:td.active_pools_12m_24m (:active_pools_12m_24m_count @data*)]
      [:td.pools-total (:pools_count @data*)]]]]])

(defn main []
  [:div.statistics-basic
   [routing/hidden-state-component
    {:did-mount fetch}]
   [users-component]
   [contracts-component]
   [items-component]
   [models-component]
   [pools-component]
   [debug-component]])
