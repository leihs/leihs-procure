(ns leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.suspension.main
  (:require
   [cljs.core.async :as async :refer [go <!]]
   [cljs.pprint :refer [pprint]]
   [leihs.admin.paths :as paths :refer [path]]
   [leihs.admin.resources.inventory-pools.inventory-pool.core :as inventory-pool]
   [leihs.admin.resources.inventory-pools.inventory-pool.delegations.delegation.core :as delegation]
   [leihs.admin.resources.inventory-pools.inventory-pool.suspension.core :as core]
   [leihs.admin.state :as state]
   [leihs.core.routing.front :as routing]
   [reagent.core :as reagent]
   [taoensso.timbre]))

(defonce data* (reagent/atom nil))

(defn delegation-page-suspension-component []
  [:div
   (let [suspension-path (path :inventory-pool-delegation-suspension
                               (some-> @routing/state* :route-params))]
     [:<>
      [routing/hidden-state-component
       {:did-mount
        #(go (reset! data* (<! (core/fetch-suspension< suspension-path))))}]
      [core/suspension-component @data*
       :update-handler
       #(go (reset! data* (<! (core/put-suspension< suspension-path %))))]])])

(defn header-component []
  [:h1 "Suspension of "
   [delegation/name-link-component]
   " in "
   [inventory-pool/name-link-component]])

(defn debug-component []
  (when (:debug @state/global-state*)
    [:section.debug
     [:hr]
     [:h2 "Page Debug"]
     [:div
      [:h3 "@data*"]
      [:pre (with-out-str (pprint @data*))]]]))

(defn page []
  [:div.inventory-pool-delegation-suspension
   [header-component]
   [delegation-page-suspension-component]
   [debug-component]])
