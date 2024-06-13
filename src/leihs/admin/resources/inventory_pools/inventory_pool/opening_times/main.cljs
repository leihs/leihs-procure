(ns leihs.admin.resources.inventory-pools.inventory-pool.opening-times.main
  (:require
   [cljs.pprint :refer [pprint]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.holidays.core :as holidays-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.holidays.main :as holidays]
   [leihs.admin.resources.inventory-pools.inventory-pool.nav :as nav]
   [leihs.admin.resources.inventory-pools.inventory-pool.workdays.core :as workdays-core]
   [leihs.admin.resources.inventory-pools.inventory-pool.workdays.main :as workdays]
   [leihs.admin.state :as state]))

(defn debug-component []
  (when (:debug @state/global-state*)
    [:<>
     [:div.inventory-pool-workdays-debug
      [:hr]
      [:div.inventory-pool-workdays-data
       [:h3 "@workdays/data*"]
       [:pre (with-out-str (pprint @workdays-core/data*))]]]
     [:div.inventory-pool-holidays-debug
      [:hr]
      [:div.inventory-pool-holidays-data
       [:h3 "@holidays/data*"]
       [:pre (with-out-str (pprint @holidays-core/data*))]]]]))

(defn page []
  [:article.inventory-pool-opening-times
   [inventory-pool/header]
   [inventory-pool/tabs]
   [:article.m-0.mb-5.container
    [:div.row
     [:div.col-6 [workdays/component]]
     [:div.col-6 [holidays/component]]]]
   [debug-component]])
