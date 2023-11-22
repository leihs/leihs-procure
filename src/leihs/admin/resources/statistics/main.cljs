(ns leihs.admin.resources.statistics.main
  (:refer-clojure :exclude [str keyword])
  (:require
   [cljs.core.async :as async :refer [<! go-loop]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.common.http-client.core :as http-client]
   [leihs.admin.common.icons :as icons]
   [leihs.admin.paths :refer [path]]
   [leihs.admin.resources.statistics.breadcrumbs :as breadcrumbs]
   [leihs.admin.state :as state]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]))

(def data* (reagent/atom {}))

(defn fetch []
  (go-loop [ks [:statistics-contracts
                :statistics-items
                :statistics-models
                :statistics-pools
                :statistics-users]]
    (when-let [k (first ks)]
      (some->
       {:url (path k)
        :chan (async/chan)}
       http-client/request :chan <!
       http-client/filter-success! :body
       (->> (swap! data* merge)))
      (recur (rest ks)))))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn val-component [k]
  [:<>
   (if-let [v (get @data* k)]
     [:span.text-monospace v]
     [:span [icons/waiting]])])

(defn section-component
  [& {:keys [k header description items]
      :or {header "Foo"
           description nil
           items {}}}]
  [:div.mt-3
   [:h3 header]
   (when description [:small description])
   [:dl.row
    (for [[k v] items]
      ^{:key k}
      [:<>
       [:dt.col-sm-4 v]
       [:dd.col-sm-2.text-right [val-component k]]
       [:div.col-sm-6]])]])

(defn contracts-component []
  [:<>
   [section-component
    :header "Contracts"
    :description "A contract is active if it is associated with an hand out or return during the given period. "
    :items {:active_contracts_0m_12m_count "Active last 12 months"
            :active_contracts_12m_24m_count "Active last 12 to 24 months"
            :contracts_count "Total"}]])

(defn items-component []
  [section-component
   :header "Items"
   :description "An item is active if it was handed out or returned during the given period."
   :items {:active_items_0m_12m_count "Active last 12 months"
           :active_items_12m_24m_count "Active last 12 to 24 months"
           :items_count "Total"}])

(defn models-component []
  [section-component
   :header "Models"
   :description "A model is active if it is associated with an item which was active during the given period."
   :items {:active_models_0m_12m_count "Active last 12 months"
           :active_models_12m_24m_count "Active last 12 to 24 months"
           :models_count "Total"}])

(defn pools-component []
  [section-component
   :header "Pools"
   :description "A pool is active if it is associated with active contracts during the given period."
   :items {:active_pools_0m_12m_count "Active last 12 months"
           :active_pools_12m_24m_count "Active last 12 to 24 months"
           :pools_count "Total"}])

(defn users-component []
  [section-component
   :header "Users"
   :description [:span
                 "An user account is active if it was used to sign in during the given period via an HTTP session. "
                 "Numbers might be incrorrect if the system has been runing a version of Leihs lower than 6 during the given period."]
   :items {:active_users_0m_12m_count "Active last 12 months"
           :active_users_12m_24m_count "Active last 12 to 24 months"
           :users_count "Total"}])

(defn page []
  [:article.statistics
   [:header.my-5
    [:h1 [icons/chart-column] " Statistics"]]
   [:section.statistics-basic
    [routing/hidden-state-component
     {:did-mount fetch}]
    [contracts-component]
    [items-component]
    [models-component]
    [pools-component]
    [users-component]
    [debug-component]]])
